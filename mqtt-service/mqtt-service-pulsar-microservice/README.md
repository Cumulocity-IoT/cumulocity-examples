# Reference MQTT Service Pulsar Integration Microservice

A simple microservice using a pulsar client to connect to Cumulocity MQTT Service. It should demonstrate how build a microservice to consume messages from devices which are connected to MQTT Service, filter & transform them and send them to Cumulocity.

It contains an end-to-end example with the following functionality:
- Connecting to MQTT Service Pulsar interface
- Creating a consumer and subscription
- An example of filtering a specific JSON message
- An example of transforming a JSON message to a C8Y Measurement
- Creating the measurements in Cumulocity

Non-functional aspects have been implemented as a reference, including:
- Fetching URL + credentials from provided environment variables
- Default configuration of timeouts
- Simple ID Handling, use deviceId from payload, otherwise use clientId to identify device.
- Correlating and storing clientIds to DeviceIds (usable for a Producer with device isolation)
- Async handling & acknowledgement of messages using Virtual Threads
- Retry of failing subscription to Apache Pulsar
- Creating a device when device doesn't exist

TODOs:
- Implementing Notification 2.0 to listen on Cumulocity Messages
- Publishing messages to MQTT Service using a Pulsar Producer
- Identity Cache Handling
- More complex transformation
- Implementing more Cumulocity API as examples

## Pre-requisites

* Java 21+
* Maven 3.9+
* A Cumulocity tenant with
  * MQTT Service is subscribed
  * Microservice hosting feature is subscribed

## Build & deploy the microservice

```shell
git clone git@github.com:Cumulocity-IoT/cumulocity-examples.git
cd mqtt-service/mqtt-service-pulsar-microservice
mvn clean package
```

Deploy the zip of the target folder to your Cumulocity tenant.

## Test the microservice

When the microservice is deployed it should state `Subscription to Pulsar successful!` in the end of the log files.
Now you can use a MQTT Client tool of your choice e.g. MQTTx and connect to MQTT Service:
- Host: `mqtt.eu-latest.cumulocity.com` (or appropriate instance you are using)
- Port: `2883` or `9883` for SSL
- Username: `<yourTenantId>/<yourUsername>`
- Password: `<yourPassword>`

Publish on topic `device/sim/message`
```json
{
  "temperature": {
    "value": 19,
    "unit": "°C"
  },
  "deviceId": "dev4711"
}
```

You should see in the log that the message is received, filtered, transformed, a device is created and a measurement sent to Cumulocity.