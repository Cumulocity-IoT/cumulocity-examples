# Simple Java Pulsar client example

TBC Introduction TBC

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

TBC Expected output TBC
