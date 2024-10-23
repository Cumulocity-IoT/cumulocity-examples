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

package com.cumulocity.agent.snmp.platform.model;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.ws.rs.core.UriBuilder;
import java.util.Map;

@Slf4j
@Getter
public class GatewayManagedObjectWrapper extends AbstractManagedObjectWrapper {

    public static final String C8Y_SNMP_GATEWAY_TYPE = "c8y_SNMP";
	public static final String C8Y_EXTERNAL_ID_TYPE = "c8y_Serial";
	public static final String C8Y_SNMP_GATEWAY = "c8y_SNMPGateway";
	public static final String C8Y_SUPPORTED_OPERATIONS = "c8y_SNMPConfiguration";

	public final String childDevicesPath;

	private SnmpCommunicationProperties SnmpCommunicationProperties;


	public GatewayManagedObjectWrapper(ManagedObjectRepresentation gatewayMo) {
		super(gatewayMo);

		this.childDevicesPath = UriBuilder.fromPath("/inventory/managedObjects/{deviceId}/childDevices").build(getId().getValue()).getPath();

		loadSnmpCommunicationProperties();
	}

	private void loadSnmpCommunicationProperties() {
		Object fragmentObj = managedObject.get(C8Y_SNMP_GATEWAY);
		if (fragmentObj instanceof Map) {
			ObjectMapper mapper = new ObjectMapper();
			SnmpCommunicationProperties = mapper.convertValue(fragmentObj, SnmpCommunicationProperties.class);
		} else {
			log.warn("Did not find correct {} fragment in the received gateway managed object {}",
					C8Y_SNMP_GATEWAY, managedObject.getName());
			SnmpCommunicationProperties = new SnmpCommunicationProperties();
		}
	}

	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SnmpCommunicationProperties {

		private String ipRange;

		private long pollingRate;

		private long transmitRate;

		private long autoDiscoveryInterval;
	}
}
