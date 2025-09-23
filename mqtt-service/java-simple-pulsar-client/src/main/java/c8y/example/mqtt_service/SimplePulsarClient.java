package c8y.example.mqtt_service;

import java.text.MessageFormat;
import java.nio.charset.StandardCharsets;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageListener;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.impl.auth.AuthenticationBasic;

public class SimplePulsarClient {
    public static void main(String[] args) throws Exception {
        // Validate command line
        if (args.length != 2) {
            System.err.println("Usage: SimplePulsarClient <tenantId> <username>");
            System.err.println("The Pulsar URL will be read from the C8Y_BASEURL_PULSAR environment variable");
            System.err.println("The password will be read from the console");
            System.exit(-1);
        }

        // Collect all the configuration properties
        final String pulsarUrl = System.getenv("C8Y_BASEURL_PULSAR");
        final String tenantId = args[0];
        final String username = args[1];
        final String password = new String(System.console().readPassword("Password for user %s/%s: ", tenantId, username));

        // Create the basic authentication credentials object.
        final AuthenticationBasic basicAuth = new AuthenticationBasic();
        basicAuth.configure(MessageFormat.format("'{'\"userId\":\"{0}/{1}\",\"password\":\"{2}\"'}'", tenantId, username, password));

        // Create a Pulsar client using the basic authentication credentials.
        // The client will *not* try to connect and authenticate immediately.
        final PulsarClient client = PulsarClient.builder()
            .serviceUrl(pulsarUrl)
            .authentication(basicAuth)
            .build();
        System.out.println("Created Pulsar client");

        // Create a simple message listener that will log some details of
        // each message received, when registered with a consumer.
        final MessageListener<byte[]> listener = new MessageListener<byte[]>() {
            @Override
            public void received(Consumer<byte[]> consumer, Message<byte[]> message) {
                final String clientId = message.getProperty("clientID");
                final String topic = message.getProperty("topic");
                System.out.println(MessageFormat.format("Received message from MQTT device {0} on MQTT topic {1}", clientId, topic));
                System.out.println(MessageFormat.format("Message payload: {0}", message.getValue()));
                System.out.println(MessageFormat.format("Message properties: {0}", message.getProperties()));
                try {
                    // Acknowledge the message
                    consumer.acknowledge(message);
                } catch (PulsarClientException e) {
                    e.printStackTrace();
                }
            }
        };

        // Create a Pulsar consumer on the from-device topic for the tenant,
        // using the listener defined above to process each message.
        // This will trigger connection and authentication by the client.
        final Consumer<byte[]> consumer = client.newConsumer(Schema.BYTES)
            .topic(MessageFormat.format("persistent://{0}/mqtt/from-device", tenantId))
            .subscriptionName("demoSubscription")
            .messageListener(listener)
            .subscribe();
        System.out.println("Created Pulsar consumer");

        // Wrap all the operations that might fail after we create the
        // durable subscription in a try-catch, so that we can delete the
        // subscription if something goes wrong.
        try {
            // Create a Pulsar producer on the to-device topic for the tenant.
            final Producer<byte[]> producer = client.newProducer(Schema.BYTES)
                .topic(MessageFormat.format("persistent://{0}/mqtt/to-device", tenantId))
                .create();
            System.out.println("Created Pulsar producer");

            // Publish a message to a single MQTT device
            producer.newMessage()
                .property("clientID", "demoClient")
                .property("topic", "demoTopic")
                .key("demoClient")
                .value("Message sent to a single device".getBytes(StandardCharsets.UTF_8))
                .send();
            System.out.println("Sent message to single device");

            // Publish a message to all MQTT devices subscribed to a topic
            producer.newMessage()
                .property("clientID", "")
                .property("topic", "demoTopic")
                .key("demoTopic")
                .value("Message sent to all subscribed devices".getBytes(StandardCharsets.UTF_8))
                .send();
            System.out.println("Sent message to all subscribed devices");

            // Pause for a minute to allow some test messages to be consumed
            Thread.sleep(60 * 1000);

            // Close the producer
            producer.close();
        }
        finally {
            // Delete the durable subscription.
            // This is only necessary if messages should *not* be retained
            // on the topic while the client is disconnected.
            consumer.unsubscribe();
        }

        // Close the other Pulsar objects that we created.
        consumer.close();
        client.close();
    }
}
