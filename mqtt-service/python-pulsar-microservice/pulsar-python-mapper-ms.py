#!/usr/bin/env python3
"""
Cumulocity Microservice with Apache Pulsar Integration
"""
from datetime import datetime
import os
import logging
import time
import json
import threading
from typing import Optional
from dotenv import load_dotenv
import pulsar
from c8y_api import CumulocityApi
from c8y_api.model import Device, Measurement
from c8y_api.app import MultiTenantCumulocityApp
from c8y_tk.app import SubscriptionListener
from flask import Flask, jsonify
from pprint import pformat
# Use the stdlib threading module for background tasks



# Configure logging
log_level = os.getenv('LOG_LEVEL', 'INFO').upper()
logging.basicConfig(
    level=getattr(logging, log_level, logging.INFO),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class PulsarCumulocityBridge:
    """Bridge between Apache Pulsar and Cumulocity IoT"""
    external_id_type = 'c8y_Serial'
    device_prefix = 'MyDevice-'
    device_type = 'mqtt_pulsar_Device'


    def __init__(self):
        load_dotenv()
        self.c8yapp: Optional[MultiTenantCumulocityApp] = None
        self._running = False
        self.pulsar_clients = {}  # tenant_id -> {'client': pulsar.Client, 'consumer': pulsar.Consumer, 'c8y_client': CumulocityApi}
        self.subscription_listener: Optional[SubscriptionListener] = None
        self.device_cache = {}  # {tenant_id -> {external_id -> Device}}
        
    def initialize_subscription_listener(self):
        """Initialize Cumulocity connection"""
        try:
            # Set up subscription listener for dynamic tenant tracking
            self.c8yapp = MultiTenantCumulocityApp()
            self.subscription_listener = SubscriptionListener(
                app=self.c8yapp,
                blocking=False,  # Run callbacks in threads
                polling_interval=10,  # Check every 5 minutes
                startup_delay=2  # Wait 30 seconds before considering a tenant "added"
            )
            
            # Add callbacks for tenant subscription changes
            self.subscription_listener.add_callback(
                callback=self.add_subscriber,
                when='added',
                blocking=False
            ).add_callback(
                callback=self.remove_subscriber,
                when='removed',
                blocking=False
            )
            
            # Start the subscription listener
            self.subscription_listener.start()
            logging.info("Subscription listener started")
            
        except Exception as e:
            logger.error(f"Failed to connect to Cumulocity: {e}")
            raise
            

    def add_subscriber(self, tenant_id: str):
        """Add a subscriber and initialize Pulsar connection"""
        try:
            
            logger.info(f"Tenant subscription event received: {tenant_id}")
            self.c8yapp = MultiTenantCumulocityApp()
            c8y_api = self.c8yapp.get_tenant_instance(tenant_id)
            logger.info(f"add_subscriber called for tenant {tenant_id}")
            time.sleep(2)  # Allow time for previous connections to fully close
            logger.info(f"Delay completed, proceeding with Pulsar initialization for tenant {tenant_id}")
            pulsar_client, consumer = self.initialize_pulsar(c8y_api)
            self.pulsar_clients[tenant_id] = {
                'client': pulsar_client,
                'consumer': consumer,
                'c8y_client': c8y_api
            }
            logger.info(f"Tenant {tenant_id} successfully added and Pulsar connection established")
        except Exception as e:
            logger.error(f"Error adding subscriber for tenant {tenant_id}: {e}")
    
    def remove_subscriber(self, tenant_id: str):
        """Remove a subscriber and cleanup Pulsar connection"""
        try:
            logger.info(f"Tenant unsubscription event received: {tenant_id}")
            logger.info(f"remove_subscriber called for tenant {tenant_id}")
            if tenant_id in self.pulsar_clients:
                logger.info(f"Found existing Pulsar connections for tenant {tenant_id}, proceeding with cleanup")
                connections = self.pulsar_clients[tenant_id]
                try:
                    if connections.get('consumer'):
                        logger.info(f"Closing Pulsar consumer for tenant {tenant_id}")
                        connections['consumer'].close()
                        time.sleep(2)  # Give more time to close consumer
                        logger.info(f"Pulsar consumer closed for tenant {tenant_id}")
                    if connections.get('client'):
                        logger.info(f"Closing Pulsar client for tenant {tenant_id}")
                        connections['client'].close()
                        logger.info(f"Pulsar client closed for tenant {tenant_id}")
                except Exception as close_error:
                    logger.error(f"Error closing Pulsar connections for tenant {tenant_id}: {close_error}")
                finally:
                    del self.pulsar_clients[tenant_id]
                    logger.info(f"Removed tenant {tenant_id} from pulsar_clients dictionary")
            else:
                logger.warning(f"Tenant {tenant_id} not found in pulsar_clients")
            logger.info(f"Tenant {tenant_id} successfully removed and Pulsar connections cleaned up")
        except Exception as e:
            logger.error(f"Error removing subscriber for tenant {tenant_id}: {e}")

    def message_listener(self, tenant_id: str, c8y_api: CumulocityApi, consumer, message):
        """Callback function for processing incoming Pulsar messages for a specific tenant"""
        try:
            # Get raw message data
            propertys = message.properties()
            client_id = propertys.get('clientID', None)
            topic = propertys.get('topic', None)

            # Filter message by topic. You could also use a regular expression here for wildcard matching.
            if not topic == 'mytopic':
                logger.info(f"Tenant {tenant_id}: Ignoring message from topic: {topic}")
                consumer.acknowledge(message)
                return
            else:
                # Process the message in a separate thread to avoid blocking the listener
                t = threading.Thread(
                    target=self.process_message,
                    args=(tenant_id, c8y_api, consumer, topic, client_id, message),
                    daemon=True,
                )
                t.start()
                # Acknowledge the message
                consumer.acknowledge(message)

        except Exception as e:
            consumer.acknowledge(message)
            logger.error(f"Tenant {tenant_id}: Error processing message: {e}")

    def process_message(self, tenant_id: str, c8y_api: CumulocityApi, consumer: pulsar.Consumer, topic: str, client_id: str, message: any):
        """Process incoming message and send to Cumulocity for a specific tenant"""
        try:
            raw_data = message.data()
            logger.info(f"Raw message (length={len(raw_data)}): {raw_data[:200]}")
            
            # Decode message
            decoded_message = raw_data.decode('utf-8')
            try:
                message_data = json.loads(decoded_message)
            except json.JSONDecodeError:
                logger.error(f"Tenant {tenant_id}: Failed to decode JSON message: {decoded_message}")
                consumer.negative_acknowledge(message)
                return

            # Now we have the challenge to check whether the device already exists in Cumulocity. If not, we have create it. 
            # We will use the the identity api (externalId) for that. The externalId has a type we will use the default type c8y_Serial for that.
            # We also assume that the clientID from Pulsar is the unique identifier for the device. If the unique identifier is located somewhere else
            # E.g. in the topic of payload you have to adjust the code accordingly.

            if client_id:
                external_id = client_id
            else:
                logger.error(f"Tenant {tenant_id}: No clientID property found in message")
                consumer.negative_acknowledge(message)
                return
            
            # Check if device exists
            device = self.get_device(c8y_api, external_id)
            if device is None:
                logger.error(f"Tenant {tenant_id}: Device with external ID {external_id} could not be created or retrieved.")
                consumer.negative_acknowledge(message)
                return
            
            # Now we have the device, we can create a measurement.
            # For this example, we assume the message contains temperature and pressure data.
            # We assume the message send to the MQTT-Service contains measurement data in a specific format like: 
            # {
            #   "timestamp": "2026-01-14T12:00:00Z",
            #   "temperature":  23.5,
            #   "pressure": 90
            # }
            # You have to adjust the code accordingly if your message format is different.

            time = datetime.fromisoformat(message_data.get('timestamp'))
            temperature = message_data.get('temperature')
            pressure = message_data.get('pressure')

            # Try creating measurement, retry once if device was deleted (422 error)
            for attempt in range(2):
                measurement = Measurement(type='TempPress', source=device.id, time=time,
                           TempPress={'temperature': {'value': temperature, 'unit': '°C'},
                                    'pressure': {'value': pressure, 'unit': 'kPa'}})
                try:
                    c8y_api.measurements.create(measurement)
                    logger.info(f"Tenant {tenant_id}: Sent measurement for device {device.id}\n{pformat(measurement.to_json())}")
                    break
                except Exception as e:
                    if attempt == 0 and ('422' in str(e) or 'Unprocessable Entity' in str(e)):
                        logger.warning(f"Tenant {tenant_id}: Device {external_id} deleted, refreshing cache")
                        if tenant_id in self.device_cache and external_id in self.device_cache[tenant_id]:
                            del self.device_cache[tenant_id][external_id]
                        device = self.get_device(c8y_api, external_id)
                        if not device:
                            raise
                    else:
                        raise
                
        except Exception as e:
            logger.error(f"Tenant {tenant_id}: Failed to process message: {e}")
            consumer.negative_acknowledge(message)

    def get_device(self, c8y_client: CumulocityApi, external_id: str) -> Optional[Device]:
        """Retrieve or create device in Cumulocity by external ID with caching"""
        tenant_id = c8y_client.tenant_id
        
        # Check device cache first
        if tenant_id in self.device_cache and external_id in self.device_cache[tenant_id]:
            logger.info(f"Tenant {tenant_id}: Device {external_id} found in cache")
            return self.device_cache[tenant_id][external_id]
        
        # Initialize cache for this tenant if not exists
        if tenant_id not in self.device_cache:
            self.device_cache[tenant_id] = {}
        
        device = None
        try:
            device = c8y_client.identity.get_object(external_id, self.external_id_type)
        except Exception as e:
            logger.error(f"No device found retrieving or creating device: {e}")
        if device is None:
            # Create new device
            try:
                device = Device(c8y_client, type=self.device_type, name=f'{self.device_prefix}{external_id}').create()
                c8y_client.identity.create(external_id, self.external_id_type, device.id)
                logger.info(f"Created new device with ID: {device.id} for external ID: {external_id}")
            except Exception as e:
                logger.error(f"Failed to create device for external ID {external_id}: {e}")
                # @todo handle creation failure, what if device creation works but external ID creation fails?
                return None
        
        # Cache the device
        if device:
            self.device_cache[tenant_id][external_id] = device
            logger.info(f"Tenant {tenant_id}: Device {external_id} cached with ID {device.id}")
        
        return device

    def initialize_pulsar(self, c8y_api: CumulocityApi):
        """Initialize Apache Pulsar connection with message listener for a specific tenant"""
        try:
            c8y_tenant = c8y_api.tenant_id
            pulsar_url = os.environ.get('C8Y_BASEURL_PULSAR')
            pulsar_topic = f'persistent://{c8y_api.tenant_id}/mqtt/from-device'
            subscription_name = f'{c8y_tenant}_pulsar-python-mapper'
            logger.info(f"Connecting to Pulsar for tenant {c8y_tenant} at {pulsar_url}")

            # Configure authentication using C8Y credentials
            auth_params = {}
            logger.info(f"Using basic authentication for user: {c8y_api.auth.username}")
            auth_params['authentication'] = pulsar.AuthenticationBasic(
                c8y_api.auth.username,
                c8y_api.auth.password
            )
            
            pulsar_client = pulsar.Client(
                pulsar_url,
                **auth_params
            )
            
            # Create tenant-specific message listener
            def tenant_message_listener(consumer: pulsar.Consumer, message):
                self.message_listener(c8y_tenant, c8y_api, consumer, message)
            
            # Subscribe with message listener for event-driven processing
            consumer: pulsar.Consumer = pulsar_client.subscribe(
                topic=pulsar_topic,
                subscription_name=subscription_name,
                consumer_type=pulsar.ConsumerType.Shared,
                message_listener=tenant_message_listener
            )
            
            logger.info(f"Successfully subscribed to Pulsar topic: {pulsar_topic} for tenant {c8y_tenant}")
            
            return pulsar_client, consumer
            
        except Exception as e:
            logger.error(f"Failed to connect to Pulsar for tenant {c8y_api.tenant_id}: {e}")
            raise
    
    def start(self):
        """Start the message listener and keep the service running"""
        self._running = True
        logger.info("Pulsar-Cumulocity bridge is running with message listener")
        logger.info("Press Ctrl+C to stop")
        
        try:
            # Keep the service running - messages are processed by the listener
            while self._running:
                time.sleep(1)
                    
        except KeyboardInterrupt:
            logger.info("Received interrupt signal")
        finally:
            self.stop()
    
    def stop(self):
        """Stop the bridge and cleanup resources"""
        self._running = False
        logger.info("Stopping Pulsar-Cumulocity bridge")
        
        # Stop subscription listener
        if self.subscription_listener:
            logger.info("Stopping subscription listener")
            try:
                self.subscription_listener.shutdown(timeout=10)
            except Exception as e:
                logger.error(f"Error stopping subscription listener: {e}")
        
        # Close all Pulsar connections
        for tenant_id, connections in self.pulsar_clients.items():
            logger.info(f"Closing Pulsar connections for tenant {tenant_id}")
            try:
                if connections['consumer']:
                    connections['consumer'].close()
                if connections['client']:
                    connections['client'].close()
            except Exception as e:
                logger.error(f"Error closing Pulsar connections for tenant {tenant_id}: {e}")
        
        self.pulsar_clients.clear()
        logger.info("Bridge stopped")


# Global reference to the bridge instance for health checks
bridge_instance = None

# Flask app for health endpoint
app = Flask(__name__)

@app.route('/health', methods=['GET'])
def health_check():
    """Simple health check endpoint for Cumulocity microservice monitoring"""
    if bridge_instance and bridge_instance._running:
        return jsonify({"status": "healthy"}), 200
    return jsonify({"status": "unhealthy"}), 503


def run_health_server():
    """Run the Flask health server in a separate thread"""
    from waitress import serve
    serve(app, host='0.0.0.0', port=80, threads=1)


def main():
    """Main entry point"""
    global bridge_instance
    
    logger.info("Initializing Pulsar-Cumulocity microservice")
    
    bridge_instance = PulsarCumulocityBridge()
    
    try:
        # Initialize connections
        bridge_instance.initialize_subscription_listener()
        
        # Start health server in background thread
        health_thread = threading.Thread(target=run_health_server, daemon=True)
        health_thread.start()
        
        # Start processing messages
        bridge_instance.start()
        
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise


if __name__ == '__main__':
    main()
