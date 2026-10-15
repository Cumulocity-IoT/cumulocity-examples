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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class DeviceProtocolManagedObjectWrapper extends AbstractManagedObjectWrapper {

	public static final String C8Y_REGISTERS = "c8y_Registers";

	Map<OID, Register> oidMap = new HashMap<>();

	List<VariableBinding> measurementVariableBindingList = new ArrayList<>();

	public DeviceProtocolManagedObjectWrapper(ManagedObjectRepresentation deviceProtocolMO) {
		super(deviceProtocolMO);

		loadRegisters();
	}

	private void loadRegisters() {
		Object registersObj = managedObject.get(C8Y_REGISTERS);
		if (registersObj instanceof List) {
			List<Register> registers = MAPPER.convertValue(registersObj,
					MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, Register.class));
			for (Register register : registers) {
				try {
					OID oid = new OID(register.getOid());
					oidMap.put(oid, register);

					if (register.getMeasurementMapping() != null) {
						VariableBinding variableBinding = new VariableBinding(oid);
						measurementVariableBindingList.add(variableBinding);
					}
				} catch(Throwable t) {
					log.error("Error while parsing the OID {} from the device protocol {}. Skipping the register.", register.getOid(), getName(), t);
				}
			}
		} else {
			log.warn("Did not find {} fragment in the received gateway managed object {}", C8Y_REGISTERS,
					managedObject.getName());
		}
	}
}
