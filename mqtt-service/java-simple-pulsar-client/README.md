# Simple MQTT Service Java client example

A simple Pulsar client for the Cumulocity MQTT Service written in Java.
This is the runnable version of the example client described in the [user documentation](https://cumulocity.com/docs/device-integration/mqtt-service/#pulsar-client) for the MQTT Service.
You should read that documentation before trying to run this example, to ensure that your tenant and user are correctly configured to use the MQTT Service.

## Pre-requisites

* Java 17+
* Maven 3.9+
* A Cumulocity tenant and user authorized to use the MQTT Service

## Running the demo

```shell
git clone git@github.com:Cumulocity-IoT/cumulocity-examples.git
cd mqtt-service/java-simple-pulsar-client
mvn clean package
export C8Y_BASEURL_PULSAR=pulsar+ssl://<DOMAIN>:6651
java -jar target/java-simple-pulsar-client-<VERSION>-jar-with-dependencies.jar <TENANT> <USER>
```

Where:
* `<DOMAIN>` is the domain of your Cumulocity tenant, e.g. `my-tenant.cumulocity.com`.
* `<VERSION>` is the version of the examples that was built, e.g. `2025.61.0-SNAPSHOT`.
* `<TENANT>` is the **ID** (not the name) of your tenant, e.g. `t123456789`.
* `<USER>` is a username in your tenant that is authorized to connect to the MQTT Service.

The client will pause for 60 seconds after publishing some messages, to allow time for MQTT devices to publish messages that will be consumed by the client.
The output should look similar to this, depending on which messages were published by devices while the demo client was running:
```
Password for user t123456789/username: 
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
Created Pulsar client
Created Pulsar consumer
Created Pulsar producer
Sent message to single device
Sent message to all subscribed devices
Received message from MQTT device demoClient on MQTT topic demoTopicA
Message payload: Message sent at time Tue Sep 23 17:51:52 2025
Message properties: {clientID=demoClient, topic=demoTopicA}
Received message from MQTT device demoClient on MQTT topic demoTopicA
Message payload: Message sent at time Tue Sep 23 17:52:02 2025
Message properties: {clientID=demoClient, topic=demoTopicA}
```
