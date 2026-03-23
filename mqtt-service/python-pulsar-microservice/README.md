# Cumulocity Pulsar Microservice

A Cumulocity IoT microservice that integrates Apache Pulsar messaging with Cumulocity IoT platform. This microservice consumes messages from Apache Pulsar topics and forwards them to Cumulocity as measurements. You can use this microservice as a starting point for building custom integrations between Pulsar and Cumulocity. You can cater the input in the process_message function to your specific use case.

## Features

- Multi-tenant support for Cumulocity IoT
- Uses the cumulocity Python SDK for Cumulocity API interactions
- Uses the Apache Pulsar Python client for Pulsar interactions
- Consumes messages send via the MQTT Service, received via the standard pulsar from device topic
- Converts the source format and sends Measurements to Cumulocity

- `timestamp`: ISO 8601 formatted timestamp
- Additional fields are treated as measurement values (e.g., `temperature`, `pressure`)

For all following scripts and cli commands, you have to set-session for the c8y CLI before running the script.

## Local Development

### Microservice Creation

If you haven't already created the microservice in your Cumulocity tenant, you can do so using the Cumulocity CLI. Use the following script to create the microservice:

```bash
./create_ms.sh
```

### Environment Variables

The microservice is configured through environment variables. For local development you need to set the following variables:

```
C8Y_BASEURL=https://<tenant-name>.eu-latest.cumulocity.com
C8Y_BOOTSTRAP_USER=servicebootstrap_pulsar-python-mapper-ms
C8Y_BOOTSTRAP_PASSWORD=<password>
C8Y_BOOTSTRAP_TENANT=<tenant-id>
C8Y_MICROSERVICE_ISOLATION=MULTI_TENANT
C8Y_BASEURL_PULSAR=pulsar+ssl://<tenant-id>.eu-latest.cumulocity.com:6651
```

You can also use the `create_env.sh` script to generate a `.env` file with the required environment variables for local development. Do set-session for the c8y CLI before running the script.

When the microservice is deployed to Cumulocity, the environment variables are automatically set based on the microservice configuration.

## Deployment to Cumulocity

Use deploy.sh script to deploy the microservice to Cumulocity:

```bash
./deploy.sh
```

The script will build the Docker image, package the image.zip and push it to the Cumulocity registry.

## Testing

at first make sure you have a tenant subscribed to the microservice. You can use the Cumulocity CLI to subscribe a tenant to the microservice:

```bash
c8y tenants applications enable -f --application pulsar-python-mapper-ms  --tenant <tenant-id>
```

The microservice expects JSON messages with measurement data in the following format:

```json
{
  "timestamp": "2026-01-14T12:00:00Z",
  "temperature": 23.5,
  "pressure": 90
}
```

Send such a message to the MQTT service of a subscribed tenant. The microservice will consume the message from the Pulsar topic

```python
f'persistent://{c8y_api.tenant_id}/mqtt/from-device'
```

On this topic, messages sent to any MQTT topics via the MQTT service are delivered to this single pulsar topic. You have to take care of from which topic and from which device you would like to process messages. You can use the message payload to identify the source device and topic. The microservice will then process the message and forward it to Cumulocity as a measurement.

## Adjusting the Message Processing

You can adjust the message processing logic in the `message_listener` and `process_message` function in `pulsar-python-mapper-ms.py` to fit your specific use case. This function is called for each message consumed from the Pulsar topic. You can parse the message payload and create measurements or other Cumulocity objects as needed.
