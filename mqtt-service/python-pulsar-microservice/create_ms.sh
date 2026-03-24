#!/bin/sh
# Using the C8Y CLI to create the microservice application with required roles

c8y applications create --key pulsar-python-mapper-ms --name pulsar-python-mapper-ms --type MICROSERVICE  --data 'requiredRoles=["ROLE_MQTT_SERVICE_MESSAGING_TOPICS_READ","ROLE_MQTT_SERVICE_MESSAGING_TOPICS_UPDATE",
    "ROLE_INVENTORY_CREATE",
    "ROLE_IDENTITY_READ",
    "ROLE_IDENTITY_ADMIN",
    "ROLE_MEASUREMENT_ADMIN"
  ]'