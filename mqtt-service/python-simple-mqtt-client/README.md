# Simple Python MQTT client example

A simple MQTT client written in Python.
This client can be used to test that your Pulsar client for the Cumulocity MQTT Service is able to exchange messages with MQTT devices. You should read the [user documentation](https://cumulocity.com/docs/device-integration/mqtt-service/#pulsar-client) for the MQTT Service before trying to run this example, to ensure that your tenant and user are correctly configured to use the MQTT Service.

## Pre-requisites

* Python 3.12+
* The `paho-mqtt` Python module
* A Cumulocity tenant and user authorized to use the MQTT Service

The provided `Pipfile` can be used by [pipenv](https://pipenv.pypa.io/en/latest/) to create a Python virtual environment with all the required dependencies installed automatically.
Using a virtual environment is optional, but highly recommended to avoid polluting your main Python environment with conflicting packages.

## Running the demo

Clone the repository:
```shell
git clone git@github.com:Cumulocity-IoT/cumulocity-examples.git
cd mqtt-service/python-simple-mqtt-client
```

If `pipenv` is available, create and activate the virtual environment:
```shell
pipenv sync
pipenv shell
```

Otherwise, manually install the `paho-mqtt` package:
```shell
pip install paho-mqtt
```

Run the example client:
```shell
./python_simple_mqtt_client.py <DOMAIN> <TENANT> <USERNAME>
```

Where:
* `<DOMAIN>` is the domain of your Cumulocity tenant, e.g. `my-tenant.cumulocity.com`.
* `<TENANT>` is the **ID** (not the name) of your tenant, e.g. `t123456789`.
* `<USER>` is a username in your tenant that is authorized to connect to the MQTT Service.

The client will publish a message to topic `demoTopicA` every 10 seconds, and subscribe to topic `demoTopicB` to receive messages published by a Pulsar client.
The output should look similar to this:
```
Password for user t123456789/admin:
Publishing message on topic demoTopicA with payload: Message sent at time Tue Sep 23 17:51:32 2025
Connected with result code Success
Publishing message on topic demoTopicA with payload: Message sent at time Tue Sep 23 17:51:42 2025
Received message on topic demoTopicB with payload: b'Message sent to a single device' and message id 1
Received message on topic demoTopicB with payload: b'Message sent to all subscribed devices' and message id 1
Publishing message on topic demoTopicA with payload: Message sent at time Tue Sep 23 17:51:52 2025
Publishing message on topic demoTopicA with payload: Message sent at time Tue Sep 23 17:52:02 2025
```

The example client will run forever unless manually interrupted, usually by entering the Control-C character in the console window.
