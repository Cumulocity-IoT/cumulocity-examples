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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceManagedObjectWrapper extends AbstractManagedObjectWrapper {

	public static final String PROTOCOL_UDP = "UDP";
	public static final String PROTOCOL_TCP = "TCP";

	public static final String C8Y_SNMP_DEVICE = "c8y_SNMPDevice";
	public static final String SNMP_DEVICE_PORT = "port";
	public static final String SNMP_DEVICE_IP = "ipAddress";
	public static final String SNMP_DEVICE_PROTOCOL = "protocol";

	private SnmpDeviceProperties properties;

	public DeviceManagedObjectWrapper(ManagedObjectRepresentation snmpDeviceMo) {
		super(snmpDeviceMo);

		loadProperties();
	}

	private void loadProperties() {
		Object fragmentObj = managedObject.get(C8Y_SNMP_DEVICE);
		if (fragmentObj != null) {
			properties = MAPPER.convertValue(fragmentObj, SnmpDeviceProperties.class);
		} else {
			log.warn("Did not find correct {} fragment in the received gateway managed object {}",
					C8Y_SNMP_DEVICE, managedObject.getName());
			properties = new SnmpDeviceProperties();
		}
	}

	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SnmpDeviceProperties {
		private int version;

		private String port;

		private String type;
		
		private String ipAddress;

		private DeviceAuthentication auth;

		public DeviceAuthentication getAuth() {
			if(auth == null) {
				auth = new DeviceAuthentication();
			}

			return auth;
		}
	}

	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DeviceAuthentication {

		private String engineId;

		private String username;

		private String privPassword;

		private String authPassword;

		private int authProtocol;

		private int privProtocol;

		private int securityLevel;
	}

	public String getDeviceProtocol() {
		if(properties.getType() == null) {
			return null;
		}

		String[] data = properties.getType().trim().split("/");
		return data[data.length - 1];
	}
}
