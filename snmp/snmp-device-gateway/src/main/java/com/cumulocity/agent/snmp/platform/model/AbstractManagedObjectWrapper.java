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

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import lombok.Getter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Getter
public abstract class AbstractManagedObjectWrapper {

	// Jackson 3 fails on null for primitive fields by default; keep the Jackson 2 behaviour of mapping null to the default value.
	protected static final ObjectMapper MAPPER = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.build();

	protected ManagedObjectRepresentation managedObject;

	AbstractManagedObjectWrapper(ManagedObjectRepresentation managedObject) {
		this.managedObject = managedObject;
	}

	public GId getId() {
		return managedObject.getId();
	}

	public String getName() {
		return managedObject.getName();
	}

	public Object getAttributeValue(String fragmentName, String attributeKey) {
		Object attributeValue = null;

		Object fragmentObj = managedObject.getAttrs().get(fragmentName);
		if (fragmentObj instanceof Map) {
			Map<?, ?> fragmentMap = (Map<?, ?>) fragmentObj;
			attributeValue = fragmentMap.get(attributeKey);
		}

		return attributeValue;
	}
}
