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
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.Maps;
import lombok.Data;
import org.joda.time.DateTime;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeasurementMapping {

	@NotNull
	private String type;

	@NotNull
	private String series;

	private Map<String, Map<?, ?>> staticFragmentsMap;

	public Map<String, Map<?, ?>> getStaticFragmentsMap() {
		return staticFragmentsMap;
	}

	@JsonSetter("staticFragments")
	public void setStaticFragments(String[] staticFragments) {
		if (staticFragments != null && staticFragments.length > 0) {
			staticFragmentsMap = new HashMap<>(staticFragments.length);
			for (String oneStaticFragment : staticFragments) {
				staticFragmentsMap.put(oneStaticFragment, Collections.EMPTY_MAP);
			}
		}
	}

	@JsonSetter("type")
	public void setType(String type) {
		if (type != null) {
			this.type = type.replace(" ", "_");
		}
	}

	@JsonSetter("series")
	public void setSeries(String series) {
		if (series != null) {
			this.series = series.replace(" ", "_");
		}
	}

	public MeasurementRepresentation buildMeasurementRepresentation(ManagedObjectRepresentation source, Object value, String unit) {
		Map<String, Object> series = Maps.newHashMap();
		if(value != null) {
			series.put("value", value);
		}
		if(unit != null && !unit.isEmpty()) {
			series.put("unit", unit);
		}

		Map<String, Object> typeMap = Maps.newHashMap();
		typeMap.put(this.getSeries(), series);

		MeasurementRepresentation newMeasurement = new MeasurementRepresentation();
		newMeasurement.setSource(source);
		newMeasurement.setDateTime(DateTime.now());
		newMeasurement.setType(this.getType());
		newMeasurement.setProperty(this.getType(), typeMap);

		Map<String, Map<?, ?>> staticFragmentsMap = this.getStaticFragmentsMap();
		if (staticFragmentsMap != null && !staticFragmentsMap.isEmpty()) {
			newMeasurement.getAttrs().putAll(staticFragmentsMap);
		}

		return newMeasurement;
	}
}