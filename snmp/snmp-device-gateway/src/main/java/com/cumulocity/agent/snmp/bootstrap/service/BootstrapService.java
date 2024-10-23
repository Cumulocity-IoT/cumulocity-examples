/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cumulocity.agent.snmp.bootstrap.service;

import c8y.*;
import com.cumulocity.agent.snmp.bootstrap.model.BootstrapReadyEvent;
import com.cumulocity.agent.snmp.bootstrap.model.CredentialsAvailableEvent;
import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.agent.snmp.platform.model.GatewayManagedObjectWrapper;
import com.cumulocity.agent.snmp.platform.model.PlatformConnectionReadyEvent;
import com.cumulocity.agent.snmp.platform.service.GatewayDataProvider;
import com.cumulocity.agent.snmp.platform.service.PlatformProvider;
import com.cumulocity.model.Agent;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.devicebootstrap.DeviceCredentialsRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BootstrapService implements InitializingBean {

	private final IdentityApi identityApi;

	private final InventoryApi inventoryApi;

	private final TaskScheduler taskScheduler;

	private final PlatformProvider platformProvider;

	private final GatewayProperties gatewayProperties;

	private final GatewayDataProvider gatewayDataProvider;

	private final ApplicationEventPublisher eventPublisher;

	private final DeviceCredentialsStoreService deviceCredentialsStoreService;

	private ScheduledFuture<?> deviceCredentialsPoller;

	@Override
	public void afterPropertiesSet() {
		scheduleDeviceCredentialsPoll();
	}

	@EventListener
	private void createDeviceIfNotExist(PlatformConnectionReadyEvent platformConnectionReadyEvent) {
		log.debug("Platform connection is ready.");

		try {
			ManagedObjectRepresentation deviceMO;

			ID deviceId = createID(gatewayProperties.getGatewayIdentifier());
			Optional<ExternalIDRepresentation> existingIdentity = getExternalID(deviceId);

			if (!existingIdentity.isPresent()) {
				deviceMO = createGatewayManagedObject();
				createExternalID(deviceId, deviceMO);
				log.info("A new device named '{}' created with id '{}'.", gatewayProperties.getGatewayIdentifier(), deviceMO.getId().getValue());
			} else {
				GId deviceMoID = existingIdentity.get().getManagedObject().getId();
				deviceMO = inventoryApi.get(deviceMoID);
				log.info("Device named '{}' is already present in the platform with id '{}'.", gatewayProperties.getGatewayIdentifier(), deviceMO.getId().getValue());

				if (!platformConnectionReadyEvent.getCurrentUser().equals(deviceMO.getOwner())) {
					log.error("Device named '{}' is owned by another user '{}', change it to '{}' and restart the agent. "
						+ "\nShutting down agent...", gatewayProperties.getGatewayIdentifier(), deviceMO.getOwner(), platformConnectionReadyEvent.getCurrentUser());
					System.exit(0);
				}
			}

			gatewayDataProvider.updateGatewayObjects(deviceMO);

			log.debug("Bootstrapping gateway device is completed.");

			eventPublisher.publishEvent(new BootstrapReadyEvent());

		} catch (BeanCreationException e) {
			if (detectInvalidCredentials(e)) {
				log.error("Invalid device credentials detected! Removing local cached credentials...");
				deviceCredentialsStoreService.remove();
				log.info("Local credentials removed! \nRestart the agent. \nShutting down the agent...");

				System.exit(0);
			}

			throw e;
		}
	}

	private boolean detectInvalidCredentials(BeanCreationException e) {
		Throwable cause = e;

		while (!Objects.isNull(cause.getCause())) {
			if (cause.getCause() instanceof SDKException) {
				SDKException sdkException = (SDKException) cause.getCause();
				if (sdkException.getHttpStatus() == HttpStatus.SC_UNAUTHORIZED) {
					return true;
				}
			}

			cause = cause.getCause();
		}

		return false;
	}

	private void scheduleDeviceCredentialsPoll() {
		deviceCredentialsPoller = taskScheduler.scheduleWithFixedDelay(() -> {
            try {
                log.info("Fetching device credentials...");

                DeviceCredentialsRepresentation deviceCredentials = deviceCredentialsStoreService.fetch();
                if (deviceCredentials != null && !gatewayProperties.isForcedBootstrap()) {
                    log.info("Device credentials are available locally");
                } else {
                    log.info("Device credentials are either unavailable locally or bootstrap is forced. Fetching them from the platform...");
                    deviceCredentials = pollDeviceCredentials();
                }

                if (deviceCredentials != null) {
                    log.info("Obtained device credentials");

                    eventPublisher.publishEvent(new CredentialsAvailableEvent(deviceCredentials));
                }
            } catch(Throwable t) {
                log.error("Unable to connect to the platform, correct the issue and restart the agent. \nShutting down the agent...", t);
                System.exit(0);
            }
		}, gatewayProperties.getBootstrapFixedDelay());
	}

	@EventListener
	private void stopDeviceCredentialsPoll(CredentialsAvailableEvent credentialsAvailableEvent) {
		if(deviceCredentialsPoller != null) {
			deviceCredentialsPoller.cancel(true);
		}
	}

	private DeviceCredentialsRepresentation pollDeviceCredentials() {
		DeviceCredentialsRepresentation deviceCredentials = null;

		try {
			deviceCredentials = platformProvider.getBootstrapPlatform().getDeviceCredentialsApi().pollCredentials(gatewayProperties.getGatewayIdentifier());
			deviceCredentialsStoreService.store(deviceCredentials);
		} catch (SDKException e) {
			if (e.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
				log.warn("A device with id {} is either not registered or not accepted. "
						+ "Register or accept a device with id {}, using Device Management user interface.", gatewayProperties.getGatewayIdentifier(), gatewayProperties.getGatewayIdentifier());
			} else if(e.getHttpStatus() == HttpStatus.SC_UNAUTHORIZED) {
				log.error("Unable to connect to the platform as incorrect bootstrap credentials were provided. Update the credentials in file:${user.home}/.snmp/snmp-agent-gateway.properties and restart the agent.\n{}\nShutting down the agent...", e.getMessage());
				log.debug(e.getMessage(), e);
				throw e;
			} else {
				log.error("Unable to connect to the platform, correct the issue and restart the agent.\n{}\nShutting down the agent...", e.getMessage());
				log.debug(e.getMessage(), e);
				throw e;
			}
		}

		return deviceCredentials;
	}

	private ID createID(String identifier) {
		return new ID(GatewayManagedObjectWrapper.C8Y_EXTERNAL_ID_TYPE, identifier);
	}

	private Optional<ExternalIDRepresentation> getExternalID(ID id) {
		try {
			ExternalIDRepresentation existingExternalId = identityApi.getExternalId(id);
			return ofNullable(existingExternalId);
		} catch (SDKException ex) {
			if (ex.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
				return empty();
			}

			throw ex;
		}
	}

	private void createExternalID(ID id, ManagedObjectRepresentation deviceMO) {
		ExternalIDRepresentation externalId = new ExternalIDRepresentation();
		externalId.setExternalId(id.getValue());
		externalId.setType(id.getType());
		externalId.setManagedObject(deviceMO);

		identityApi.create(externalId);
	}

	private ManagedObjectRepresentation createGatewayManagedObject() {
		SupportedOperations operation = new SupportedOperations();
		operation.add(GatewayManagedObjectWrapper.C8Y_SUPPORTED_OPERATIONS);

		ManagedObjectRepresentation deviceMO = new ManagedObjectRepresentation();
		deviceMO.setName(gatewayProperties.getGatewayIdentifier());
		deviceMO.setType(GatewayManagedObjectWrapper.C8Y_SNMP_GATEWAY_TYPE);
		deviceMO.set(new Agent());
		deviceMO.set(new IsDevice());
		deviceMO.set(new Hardware());
		deviceMO.set(new Mobile());
		deviceMO.set(new Object(), GatewayManagedObjectWrapper.C8Y_SNMP_GATEWAY);
		deviceMO.set(new RequiredAvailability(gatewayProperties.getGatewayAvailabilityInterval()));
		deviceMO.set(operation);

		return inventoryApi.create(deviceMO);
	}
}
