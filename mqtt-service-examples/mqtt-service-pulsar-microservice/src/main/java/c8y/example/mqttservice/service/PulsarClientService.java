package c8y.example.mqttservice.service;

import c8y.example.mqttservice.callback.PulsarCallback;
import c8y.example.mqttservice.client.C8YClient;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionRemovedEvent;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.auth.AuthenticationBasic;
import org.apache.pulsar.shade.com.google.gson.JsonObject;
import org.apache.pulsar.shade.com.google.gson.JsonParser;
import org.apache.pulsar.shade.com.google.gson.JsonSyntaxException;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PulsarClientService {

    // Pulsar message properties
    public static final String PULSAR_PROPERTY_TOPIC = "topic";
    public static final String PULSAR_PROPERTY_CHANNEL = "channel";
    public static final String PULSAR_PROPERTY_CLIENT_ID = "clientID";

    // Topic names
    public static final String PULSAR_TO_DEVICE_TOPIC = "to-device";
    public static final String PULSAR_FROM_DEVICE_TOPIC = "from-device";
    public static final String PULSAR_NAMESPACE = "mqtt";

    //Default configuration
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30;
    private static final int DEFAULT_OPERATION_TIMEOUT = 30;
    private static final int DEFAULT_KEEP_ALIVE = 30;
    private static final int MAX_PRODUCER_CREATE_RETRIES = 3;
    private static final int PRODUCER_CREATE_RETRY_DELAY_MS = 1000;

    //FIXME Change this to an unique subscription name
    private static final String SUBSCRIPTION_NAME = "MQTT_SERVICE_PULSAR_EXAMPLE_SUBSCRIPTION";

    protected PulsarClient pulsarClient;

    //This map is used to manage one client per tenant
    private final HashMap<String, PulsarClient> clientMap = new HashMap<>();
    //This map is used to manage one callback per tenant
    private final HashMap<String, PulsarCallback> callbackMap = new HashMap<>();
    //This map is used to correlate device IDs to clientIDs
    private final HashMap<String, String> deviceClientIdMap = new HashMap<>();
    private final HashMap<String, Consumer> consumerMap = new HashMap<>();

    @Value("${C8Y_BASEURL_PULSAR:}")
    @Getter
    String mqttServicePulsarUrl;

    @Autowired
    C8YClient c8YClient;

    @Autowired
    MicroserviceSubscriptionsService subscriptionsService;

    @Bean("virtualThreadPool")
    public ExecutorService virtualThreadPool() {
        final ThreadFactory factory = Thread.ofVirtual().name("virtThread-", 0).factory();
        return Executors.newThreadPerTaskExecutor(factory);
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
            createConsumer(tenant, SUBSCRIPTION_NAME, clientMap.get(tenant), callbackMap.get(tenant));
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
        String authParams = MessageFormat.format(
                "'{'\"userId\":\"{0}/{1}\",\"password\":\"{2}\"'}'",
                tenant, credentials.getUsername(), credentials.getPassword());
        final AuthenticationBasic basicAuth = new AuthenticationBasic();
        basicAuth.configure(authParams);

        // Create a Pulsar client using the basic authentication credentials.
        // The client will *not* try to connect and authenticate immediately.
        final PulsarClient client = PulsarClient.builder()
                .serviceUrl(mqttServicePulsarUrl)
                .authentication(basicAuth)
                .connectionTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .operationTimeout(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS)
                .keepAliveInterval(DEFAULT_KEEP_ALIVE, TimeUnit.SECONDS)
                .build();
        clientMap.put(tenant, client);
        PulsarCallback callback = new PulsarCallback(tenant, virtualThreadPool(), this);
        callbackMap.put(tenant, callback);
    }

    public Consumer<byte[]> createConsumer(String tenant, String subscriptionName, PulsarClient client, PulsarCallback callback) throws PulsarClientException {
        String fromDevice = String.format("persistent://%s/%s/%s",
                tenant, PULSAR_NAMESPACE, PULSAR_FROM_DEVICE_TOPIC);
        final Consumer<byte[]> consumer = client.newConsumer(Schema.BYTES)
                .topic(fromDevice)
                .subscriptionName(subscriptionName)
                .messageListener(callback)
                .subscribe();
        consumerMap.put(tenant, consumer);
        return consumer;
    }

    public Producer<byte[]> createProducer(String tenant, PulsarClient client, PulsarCallback callback) throws PulsarClientException {
        String toDevice = String.format("persistent://%s/%s/%s",
                tenant, PULSAR_NAMESPACE, PULSAR_TO_DEVICE_TOPIC);
        final Producer<byte[]> producer = client.newProducer(Schema.BYTES)
                .topic(toDevice)
                .create();
        return producer;
    }

    public void processMessage(String tenant, Message<byte[]> msg, String clientId) throws JsonSyntaxException {
        log.info("{} - Processing message {}", tenant, msg);
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
            if(jsonObject.has("temperature")) {
                JsonObject temperatureObject = jsonObject.get("temperature").getAsJsonObject();
                unit = temperatureObject.get("unit").getAsString();
                value = temperatureObject.get("value").getAsBigDecimal();
            } else {
                unit = null;
                value = null;
            }
            if(jsonObject.has("time")) {
                time = DateTime.parse(jsonObject.get("time").getAsString());
            } else {
                time = DateTime.now();
            }
            if(jsonObject.has("deviceId")) {
                deviceId = jsonObject.get("deviceId").getAsString();
                deviceClientIdMap.put(deviceId, clientId);
            } else {
                deviceId = clientId;
            }
            //Validation
            if(value == null) {
                log.error("{} - Measurement validation failed, no value provided!", tenant);
                return;
            }

            subscriptionsService.runForTenant(tenant, () -> {
                ExternalIDRepresentation extId = c8YClient.retrieveExternalId(tenant, "c8y_Serial", deviceId);
                ManagedObjectRepresentation mor;
                if(extId == null) {
                    mor = c8YClient.createDevice(tenant,"MQTT Service Example Device "+deviceId, deviceId, "c8y_MQTTServiceExampleDevice");
                } else {
                    mor = extId.getManagedObject();
                }
                c8YClient.createSimpleMeasurement(tenant, mor, type, time, value, unit);
            });

        }
    }
}


