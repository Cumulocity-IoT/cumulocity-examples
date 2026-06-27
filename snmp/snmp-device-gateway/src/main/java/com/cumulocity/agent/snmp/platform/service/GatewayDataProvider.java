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

package com.cumulocity.agent.snmp.platform.service;

import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.agent.snmp.platform.model.*;
import com.cumulocity.agent.snmp.util.IpAddressUtil;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectReferenceCollectionRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class GatewayDataProvider {

	@Autowired
	private InventoryApi inventoryApi;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private PlatformProvider platformProvider;

	@Autowired
	private GatewayProperties gatewayProperties;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Getter
	private GatewayManagedObjectWrapper gatewayDevice;

	@Getter
	private Map<String, DeviceManagedObjectWrapper> snmpDeviceMap = new HashMap<>();

	@Getter
	private Map<String, DeviceProtocolManagedObjectWrapper> protocolMap = new HashMap<>();

	public void updateGatewayObjects(ManagedObjectRepresentation gatewayManagedObject) {
		gatewayDevice = new GatewayManagedObjectWrapper(gatewayManagedObject);

		log.debug("Obtaining gateway device children and its corresponding device protocol mapping");
		refreshGatewayObjects();

		scheduleGatewayDataRefresh();
	}

	public synchronized void refreshGatewayObjects() {
		Map<String, DeviceManagedObjectWrapper> newSnmpDeviceMap = new HashMap<>();
		Map<String, DeviceProtocolManagedObjectWrapper> newProtocolMap = new HashMap<>();

		GId id = gatewayDevice.getId();
		ManagedObjectRepresentation newGatewayDevice = inventoryApi.get(id);
		ManagedObjectReferenceCollectionRepresentation deviceCollections = newGatewayDevice.getChildDevices();
		GatewayManagedObjectWrapper newGatewayDeviceWrapper = new GatewayManagedObjectWrapper(newGatewayDevice);

		deviceCollections.forEach(childDeviceRep -> {
			try {
				ManagedObjectRepresentation childDeviceMo = inventoryApi.get(childDeviceRep.getManagedObject().getId());
				if (childDeviceMo.hasProperty(DeviceManagedObjectWrapper.C8Y_SNMP_DEVICE)) {
					updateDeviceProtocol(newSnmpDeviceMap, newProtocolMap, childDeviceMo);
				}
			} catch (SDKException sdkException) {
				if (sdkException.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
					log.error(
							"Unable to find child device object {} or its device protocol mapping object from platform",
							childDeviceRep.getManagedObject().getName());
				} else {
					throw sdkException;
				}
			}
		});

		gatewayDevice = newGatewayDeviceWrapper;
		snmpDeviceMap = newSnmpDeviceMap;
		protocolMap = newProtocolMap;
	}

	void scheduleGatewayDataRefresh() {
		taskScheduler.scheduleWithFixedDelay(() -> {
			if (platformProvider.isPlatformAvailable()) {
				try {
					log.debug("Refreshing gateway managed objects...");

					refreshGatewayObjects();

					eventPublisher.publishEvent(new GatewayDataRefreshedEvent(gatewayDevice));

					log.debug("Refreshing gateway managed objects completed.");
				} catch (Throwable t) {
					// Forcefully catching throwable, as we do not want to stop the scheduler on exception.
					log.error("Unable to refresh gateway managed objects", t);

					if (isExceptionDueToInvalidCredentials(t)) {
						log.error("Invalid gateway device credentials detected. "
								+ "It could be that the gateway device was removed. "
								+ "Please bootstrap the gateway device again."
								+ "\nShutting down the gateway process.");
						System.exit(0);
					}
				}
			} else {
				log.debug("Platform is unavailable. Waiting for {} minutes before retrying gateway data refresh.",
						gatewayProperties.getGatewayObjectRefreshIntervalInMinutes());
			}
		}, Duration.ofMinutes(gatewayProperties.getGatewayObjectRefreshIntervalInMinutes()));
	}

	private void updateDeviceProtocol(Map<String, DeviceManagedObjectWrapper> newSnmpDeviceMap,
			Map<String, DeviceProtocolManagedObjectWrapper> newProtocolMap, ManagedObjectRepresentation snmpDeviceMo) {

		DeviceManagedObjectWrapper childDeviceWrapper = new DeviceManagedObjectWrapper(snmpDeviceMo);
		String deviceIp;
		try {
			deviceIp = IpAddressUtil.sanitizeIpAddress(childDeviceWrapper.getProperties().getIpAddress());
		} catch(IllegalArgumentException iae) {
			log.error("Invalid IP Address <{}> specified in the SNMP device named {}.",
					childDeviceWrapper.getProperties().getIpAddress(),
					snmpDeviceMo.getName(), iae);

			deviceIp = childDeviceWrapper.getProperties().getIpAddress();
		}
		newSnmpDeviceMap.put(deviceIp, childDeviceWrapper);

		String protocolName = childDeviceWrapper.getDeviceProtocol();
		if (protocolName != null && !protocolName.isEmpty()) {
			updateProtocolMap(protocolName, newProtocolMap);
		} else {
			log.error("Missing device protocol configuration for the SNMP device {}", snmpDeviceMo.getName());
		}
	}

	private void updateProtocolMap(String protocolName, Map<String, DeviceProtocolManagedObjectWrapper> newProtocolMap) {

		DeviceProtocolManagedObjectWrapper deviceProtocolWrapper;

		if (!newProtocolMap.containsKey(protocolName)) {
			try {
				GId deviceProtocolId = new GId(protocolName);
				ManagedObjectRepresentation deviceProtocolMo = inventoryApi.get(deviceProtocolId);
				deviceProtocolWrapper = new DeviceProtocolManagedObjectWrapper(deviceProtocolMo);
				newProtocolMap.put(protocolName, deviceProtocolWrapper);
			} catch (SDKException sdk) {
				if (sdk.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
					log.error("{} device protocol managed object not found in the platform "
							+ "but configured in the device.", protocolName, sdk);
				} else {
					log.error(sdk.getMessage(), sdk);
					throw sdk;
				}
			}
		}
	}

	private boolean isExceptionDueToInvalidCredentials(Throwable cause) {
		while (!Objects.isNull(cause)) {
			if (cause instanceof SDKException) {
				SDKException sdkException = (SDKException) cause;
				if (sdkException.getHttpStatus() == HttpStatus.SC_UNAUTHORIZED) {
					return true;
				}
			}

			cause = cause.getCause();
		}

		return false;
	}
}
