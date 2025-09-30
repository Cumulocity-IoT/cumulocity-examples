# Cumulocity MQTT Service examples

This folder contains example microservices and standalone clients for the Cumulocity MQTT Service.
See the [user guide](https://cumulocity.com/docs/device-integration/mqtt-service/) for more information on this component of the Cumulocity platform.

Each example is in its own subfolder with a separate README file explaining how to build and run the example:

* [java-simple-pulsar-client](java-simple-pulsar-client/): A basic standalone Java client application showing how to use the Pulsar API to exchange messages with MQTT devices.
* [python-simple-mqtt-client](python-simple-mqtt-client/): A simple Python MQTT client that can be used to simulate an MQTT device.

## Pre-requisites

The Java-based examples will generally require Java version 17+ and Maven version 3.9+ to build and run them.
The Maven [pom.xml](pom.xml) file in this folder will build all of the examples:

```shell
git clone git@github.com:Cumulocity-IoT/cumulocity-examples.git
cd mqtt-service
mvn clean package
```

Python scripts will generally require Python version 3.12+ to run them.
Some Python scripts will use the [paho-mqtt](https://pypi.org/project/paho-mqtt/) library and other third-party packages, which will need to be installed in your Python (virtual) environment.
A `Pipfile` for [pipenv](https://pipenv.pypa.io/en/latest/) will be provided with those examples, to create a Python virtual environment with all the required dependencies installed automatically.
