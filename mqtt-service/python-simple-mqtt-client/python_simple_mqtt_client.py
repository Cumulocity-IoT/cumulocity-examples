#!/usr/bin/env python

import paho.mqtt.client as mqtt

import getpass
import time
import sys

# Callback function invoked on successful connect
def on_connect(client, userdata, flags, reason_code, properties):
    print(f'Connected with result code {reason_code}')

# Callback function invoked for each received message
def on_message(client, userdata, msg):
    print(f'Received message on topic {msg.topic} with payload: {str(msg.payload)} and message id {msg.mid}')

# Validate command line
if len(sys.argv) < 4:
   print('Usage: ./python_simple_mqtt_client.py <server> <tenant> <username>')
   print('The password for the tenant user will be read from the console')
   sys.exit(-1)

# Collect all the configuration properties
server = sys.argv[1]
tenantId = sys.argv[2]
username = sys.argv[3]
password = getpass.getpass(f'Password for user {tenantId}/{username}: ')
topic = 'demoTopic'

# Create and configure a new MQTT client using basic authentication.
# The client will not attempt to connect and authenticate immediately.
client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION2, client_id='demoClient', clean_session=True, manual_ack=False)
client.on_connect = on_connect
client.on_message = on_message
client.username = f'{tenantId}/{username}'
client.password = password
client.tls_set()

# Connect the MQTT client to the server and start processing received
# received messages in the background
client.connect(server, 9883)
client.loop_start()

# Subscribe to the topic
client.subscribe(topic, qos=1)

# Send a message every second forever
while True:
   now = time.time()
   payload = f'Message sent at time {time.ctime(now)}'
   print(f'Publishing message on topic {topic} with payload: {payload}')
   client.publish(topic, payload, qos=1)
   time.sleep(1.0-(time.time()-now))
