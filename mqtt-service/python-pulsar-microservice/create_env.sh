#!/bin/sh
# Usage: ./create_env.sh <microservice-name>
# Creates a .env file with the necessary environment variables for the microservice
# Requires the C8Y CLI to be installed and configured
MS_NAME=$1
c8y microservices serviceusers get --id $MS_NAME \
| c8y template execute \
  --template "'C8Y_USER=%s\nC8Y_PASSWORD=%s\nC8Y_TENANT=%s\n' % [input.value.name, input.value.password, input.value.tenant]" > .env
c8y currenttenant get | c8y template execute --template "'C8Y_BASEURL=https://%s' % [input.value.domainName]" >> .env

c8y microservices getBootstrapUser --id $MS_NAME \
| c8y template execute \
  --template "'C8Y_BOOTSTRAP_USER=%s\nC8Y_BOOTSTRAP_PASSWORD=%s\nC8Y_BOOTSTRAP_TENANT=%s\n' % [input.value.name, input.value.password, input.value.tenant]" >> .env

echo C8Y_MICROSERVICE_ISOLATION=MULTI_TENANT>> .env
c8y currenttenant get | c8y template execute --template "'C8Y_BASEURL_PULSAR=pulsar+ssl://%s.eu-latest.cumulocity.com:6651' % [input.value.name]" >> .env