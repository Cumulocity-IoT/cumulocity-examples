package c8y.example.mqttservice.callback;

import c8y.example.mqttservice.service.PulsarClientService;
import com.cumulocity.sdk.client.SDKException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

@Slf4j
public class PulsarCallback implements MessageListener<byte[]>
{
    private final String tenant;
    private final ExecutorService virtualThreadPool;
    private PulsarClientService pulsarClientService;

    public PulsarCallback(String tenant, ExecutorService virtualThreadPool, PulsarClientService pulsarService) {
        this.tenant = tenant;
        this.virtualThreadPool = virtualThreadPool;
        this.pulsarClientService = pulsarService;
    }

    @Override
    public void received(Consumer<byte[]> consumer, Message<byte[]> msg) {
        //This is in most cases "from-device" when the message was originated by a device
        String internalMQTTTServiceTopic = msg.getTopicName();
        //This is the MQTT Topic used by the device and provided as message property
        String topic = msg.getProperty(PulsarClientService.PULSAR_PROPERTY_TOPIC);
        //This is the clientID who originally sent the message
        String client = msg.getProperty(PulsarClientService.PULSAR_PROPERTY_CLIENT_ID);
        //This is the raw-message as byte-array
        String payload = new String(msg.getData(), StandardCharsets.UTF_8);

        log.info("{} - Received message {} from MQTT device {} on MQTT topic {} with payload: {}", tenant, msg.getMessageId(), client, topic, payload);

        // From here we should ideally process the message asynchronously and unblock the callback-thread because we could receive a lot of messages here
        virtualThreadPool.submit(() -> {
            pulsarClientService.processMessage(tenant, consumer, msg);
        });
    }

    @Override
    public void reachedEndOfTopic(Consumer<byte[]> consumer) {
        MessageListener.super.reachedEndOfTopic(consumer);
    }
}
