package c8y.example.mqttservice.service;

import c8y.example.mqttservice.callback.PulsarCallback;
import c8y.example.mqttservice.client.C8YClient;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionRemovedEvent;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.sdk.client.SDKException;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.auth.AuthenticationBasic;
import org.apache.pulsar.shade.com.google.gson.JsonObject;
import org.apache.pulsar.shade.com.google.gson.JsonParser;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PulsarClientService {

    // Pulsar message properties
    public static final String PULSAR_PROPERTY_TOPIC = "topic";
    public static final String PULSAR_PROPERTY_CLIENT_ID = "clientID";

    // Topic names
    public static final String PULSAR_TO_DEVICE_TOPIC = "to-device";
    public static final String PULSAR_FROM_DEVICE_TOPIC = "from-device";
    public static final String PULSAR_NAMESPACE = "mqtt";

    //Default configuration
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30;
    private static final int DEFAULT_OPERATION_TIMEOUT = 30;
    private static final int DEFAULT_KEEP_ALIVE = 30;

    //FIXME Change this to an unique subscription name
    private static final String SUBSCRIPTION_NAME = "MQTT_SERVICE_PULSAR_EXAMPLE_SUBSCRIPTION";

    //This map is used to manage one client per tenant
    private final HashMap<String, PulsarClient> clientMap = new HashMap<>();
    //This map is used to manage one callback per tenant
    private final HashMap<String, PulsarCallback> callbackMap = new HashMap<>();
    //This map is used to correlate device IDs to clientIDs
    private final HashMap<String, String> deviceClientIdMap = new HashMap<>();
    private final HashMap<String, Consumer> consumerMap = new HashMap<>();


    @Getter
    private final String pulsarUrl;

    private final C8YClient c8YClient;

    private final MicroserviceSubscriptionsService subscriptionsService;

    private final RetryTemplate subscriptionRetryTemplate;

    private final ExecutorService virtualThreadPool;

    public PulsarClientService(@Value("${C8Y_BASEURL_PULSAR:}") String pulsarUrl,
                               C8YClient c8YClient,
                               MicroserviceSubscriptionsService subscriptionsService,
                               RetryTemplate subscriptionRetryTemplate,
                               @Named("virtualThreadPool") ExecutorService virtualThreadPool) {
        this.pulsarUrl = pulsarUrl;
        this.c8YClient = c8YClient;
        this.subscriptionsService = subscriptionsService;
        this.subscriptionRetryTemplate = subscriptionRetryTemplate;
        this.virtualThreadPool = virtualThreadPool;
    }

    /* Will be executed each time a tenant is subscribed and on microservice start */
    @EventListener
    public void subscribeTenant(MicroserviceSubscriptionAddedEvent event) {
        String tenant = event.getCredentials().getTenant();
        log.info("{} - Microservice subscribed", tenant);
        try {
            //Step 1: Initialize Pulsar Client per tenant
            initializePulsarClientForTenant(tenant, event.getCredentials());
            //Step 2: Create a consumer and subscribe to pulsar
            subscriptionRetryTemplate.execute(context -> {
                if (context.getRetryCount() > 0)
                    log.info("{} - Retrying to subscribe to Puslar...", tenant);
                createConsumer(tenant, SUBSCRIPTION_NAME, clientMap.get(tenant), callbackMap.get(tenant));
                return null;
            });

        } catch (Exception e) {
            log.error("{} - Initialization error: {}", tenant, e.getMessage(), e);
        }

    }

    /* Will be called when microservice is shutdown for any reasons */
    @PreDestroy
    public void disconnect() {
        clientMap.forEach((tenant, client) -> {
            try {
                client.close();
            } catch (PulsarClientException e) {
                log.error("{} - Error shutting down pulsar clients", tenant, e);
            }
        });
    }

    @EventListener
    public void removeTenant(MicroserviceSubscriptionRemovedEvent event) {
        String tenant = event.getTenant();
        try {
            PulsarClient client = clientMap.get(tenant);
            Consumer consumer = consumerMap.get(tenant);
            //Calling unsubscribe will make sure that no further messages are retained on the broker for this microservice
            consumer.unsubscribe();
            client.close();
            clientMap.remove(tenant);
            consumerMap.remove(tenant);
            callbackMap.remove(tenant);
        } catch (PulsarClientException e) {
            log.error("{} - Error shutting down pulsar clients", tenant, e);
        }

    }

    public void initializePulsarClientForTenant(String tenant, MicroserviceCredentials credentials) throws PulsarClientException {
        //Retrieve service user credentials on microservice subscription
        final AuthenticationBasic basicAuth = new AuthenticationBasic();
        basicAuth.configure(Map.of(
                "userId", "%s/%s".formatted(tenant, credentials.getUsername()),
                "password", credentials.getPassword()
        ));

        // Create a Pulsar client using the basic authentication credentials.
        // The client will *not* try to connect and authenticate immediately.
        final PulsarClient client = PulsarClient.builder()
                .serviceUrl(pulsarUrl)
                .authentication(basicAuth)
                .connectionTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .operationTimeout(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS)
                .keepAliveInterval(DEFAULT_KEEP_ALIVE, TimeUnit.SECONDS)
                .build();
        clientMap.put(tenant, client);
        PulsarCallback callback = new PulsarCallback(tenant, virtualThreadPool, this);
        callbackMap.put(tenant, callback);
    }

    public Consumer<byte[]> createConsumer(String tenant, String subscriptionName, PulsarClient client, PulsarCallback callback) throws PulsarClientException {
        log.info("{} - Creating and subscribing consumer to Pulsar ...", tenant);
        String fromDevice = String.format("persistent://%s/%s/%s",
                tenant, PULSAR_NAMESPACE, PULSAR_FROM_DEVICE_TOPIC);
        final Consumer<byte[]> consumer = client.newConsumer(Schema.BYTES)
                .topic(fromDevice)
                .subscriptionName(subscriptionName)
                .messageListener(callback)
                //worth adding so in case of update we won't be blocked by Exclusive consumer exception when new instance will start and the old one is still running
                .subscriptionType(SubscriptionType.Failover)
                .subscribe();
        log.info("{} - Subscription to Pulsar successful!", tenant);
        consumerMap.put(tenant, consumer);
        return consumer;
    }

    public Producer<byte[]> createProducer(String tenant, PulsarClient client, PulsarCallback callback) throws PulsarClientException {
        String toDevice = String.format("persistent://%s/%s/%s",
                tenant, PULSAR_NAMESPACE, PULSAR_TO_DEVICE_TOPIC);
        final Producer<byte[]> producer = client.newProducer(Schema.BYTES)
                .topic(toDevice)
                .sendTimeout(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS)
                .autoUpdatePartitions(false)
                .create();
        return producer;
    }

    public void processMessage(String tenant, Consumer<byte[]> consumer, Message<byte[]> msg) {
        /* Step 1: Filter the message  */
        //Filter on topic level but filter could also be implemented on payload or client ID level
        //This is in most cases "from-device" when the message was originated by a device
        String internalMQTTTServiceTopic = msg.getTopicName();
        //This is the MQTT Topic used by the device and provided as message property
        String topic = msg.getProperty(PulsarClientService.PULSAR_PROPERTY_TOPIC);
        //This is the clientID who originally sent the message
        String client = msg.getProperty(PulsarClientService.PULSAR_PROPERTY_CLIENT_ID);
        //This is the raw-message as byte-array
        String payload = new String(msg.getData(), StandardCharsets.UTF_8);
        try {
            if (topic.equals("device/sim/message")) {
                log.info("{} - Message {} is flagged as to be processed", tenant, msg.getMessageId());
                //Step 2: Transform message(s) to target format
                //Step 3: Send message to target API(s)
                try {
                    transformAndSendMessage(tenant, msg, client);
                    //Step 4: Acknowledge message after successful processing
                    log.info("{} - Processing of message {} successful!", tenant, msg.getMessageId());
                    consumer.acknowledge(msg);
                } catch (SDKException e) {
                    log.error("{} - Error transforming and sending message", tenant, e);
                    //For temporary errors like 5xx we should negative ack for a potential retry
                    if (e.getHttpStatus() >= 500) {
                        consumer.negativeAcknowledge(msg);
                    }
                }
            } else {
                //Acknowledge all other messages but ignore them for processing
                log.info("{} - Message {} will be ignored for processing ", tenant, msg.getMessageId());
                consumer.acknowledge(msg);
            }
        } catch (PulsarClientException e) {
            log.error("{} - Error acking message: ", tenant, e);
        }
    }

    public void transformAndSendMessage(String tenant, Message<byte[]> msg, String clientId) throws RuntimeException {
        //Here we assume we just receive JSON Format and Objects in the following format:
        /**
         {
         "temperature": {
         "value": 19,
         "unit": "°C"
         },
         "time": "2026-01-13T12:00:00.000Z",
         "deviceId": "dev4711"
         }
         **/
        if (msg.getData() != null) {
            JsonObject jsonObject = JsonParser.parseString(new String(msg.getData())).getAsJsonObject();
            String unit;
            BigDecimal value;
            DateTime time;
            String deviceId;
            String type = "c8y_TemperatureMeasurement";
            String name = "c8y_TemperatureMeasurement";
            String extIdType = "c8y_Serial";
            if (jsonObject.has("temperature")) {
                JsonObject temperatureObject = jsonObject.get("temperature").getAsJsonObject();
                unit = temperatureObject.get("unit").getAsString();
                value = temperatureObject.get("value").getAsBigDecimal();
            } else {
                unit = null;
                value = null;
            }
            if (jsonObject.has("time")) {
                time = DateTime.parse(jsonObject.get("time").getAsString());
            } else {
                time = DateTime.now();
            }
            //In this case the deviceId is part of the payload so we use it here - otherwise we use the clientId
            if (jsonObject.has("deviceId")) {
                deviceId = jsonObject.get("deviceId").getAsString();
                deviceClientIdMap.put(deviceId, clientId);
            } else {
                deviceId = clientId;
            }
            //Validation
            if (value == null) {
                log.error("{} - Measurement validation failed, no value provided!", tenant);
                return;
            }

            subscriptionsService.runForTenant(tenant, () -> {
                ExternalIDRepresentation extId = c8YClient.retrieveExternalId(tenant, extIdType, deviceId);
                ManagedObjectRepresentation mor;
                if (extId == null) {
                    log.info("{} - Device with id {} does not exists, creating it", tenant, deviceId);
                    mor = c8YClient.createDevice(tenant, "MQTT Service Example Device " + deviceId, deviceId, "c8y_MQTTServiceExampleDevice", extIdType);
                } else {
                    log.info("{} - Device with id {} already exists", tenant, deviceId);
                    mor = extId.getManagedObject();
                }
                MeasurementRepresentation measurement = c8YClient.createSimpleMeasurement(tenant, mor, name, type, time, value, unit);
            });

        }
    }
}


