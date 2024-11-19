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

package com.cumulocity.agent.snmp.platform.pubsub.subscriber;

import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.agent.snmp.platform.pubsub.service.EventPubSub;
import com.cumulocity.agent.snmp.platform.service.GatewayDataProvider;
import com.cumulocity.agent.snmp.platform.service.PlatformProvider;
import com.cumulocity.sdk.client.event.EventApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventSubscriber extends Subscriber<EventPubSub> {

	private final GatewayProperties gatewayProperties;
	private final EventApi eventApi;

	public EventSubscriber(GatewayDataProvider gatewayDataProvider, PlatformProvider platformProvider,
						   EventPubSub pubSub, GatewayProperties gatewayProperties, EventApi eventApi) {
		super(gatewayDataProvider, platformProvider, pubSub);
		this.gatewayProperties = gatewayProperties;
		this.eventApi = eventApi;
	}

	@Override
	public int getConcurrentSubscriptionsCount() {
		// 10% of the total threads available for gateway
		int count = gatewayProperties.getGatewayThreadPoolSize() * 10 / 100;

		return (count <= 0) ? 1 : count;
	}

	@Override
	public boolean isBatchingSupported() {
		return false;
	}

	@Override
	public int getBatchSize() {
		throw new UnsupportedOperationException("Batching is not supported for Events");
	}

	@Override
	public void handleMessage(String message) {
		eventApi.create(new EventRepresentation(message));
	}

	public static class EventRepresentation extends com.cumulocity.rest.representation.event.EventRepresentation {

		private String jsonString;

		public EventRepresentation() {
		}

		EventRepresentation(String jsonString) {
			this.jsonString = jsonString;
		}

		@Override
		public String toJSON() {
			return jsonString;
		}
	}
}
