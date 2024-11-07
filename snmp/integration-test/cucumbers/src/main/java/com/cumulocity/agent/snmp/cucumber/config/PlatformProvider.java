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

package com.cumulocity.agent.snmp.cucumber.config;

import com.cumulocity.agent.snmp.cucumber.config.GatewayIntegrationTestProperties.TenantProperties;
import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.PlatformBuilder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class PlatformProvider {

	@Autowired
	private TenantProperties mgmtTenantProperties;

	@Autowired
	private GatewayIntegrationTestProperties gatewayProperties;

	@Getter
	private Platform mgmtPlatform;

	@Getter
	private Platform testPlatform;

	@PostConstruct
	public Platform switchToManagement() {
		CumulocityBasicCredentials credentials = CumulocityBasicCredentials.builder()
				.tenantId(mgmtTenantProperties.getTenantId()).username(mgmtTenantProperties.getUsername())
				.password(mgmtTenantProperties.getPassword()).build();
		mgmtPlatform = createPlatform(credentials);
		return mgmtPlatform;
	}

	public Platform createIntegrationTestPlatform(CumulocityBasicCredentials testTenantCredentials) {
		testPlatform = createPlatform(testTenantCredentials);
		return testPlatform;
	}

	public Platform createPlatform(CumulocityBasicCredentials credentials) {
		return PlatformBuilder.platform().withBaseUrl(gatewayProperties.getBaseUrl())
				.withForceInitialHost(gatewayProperties.isForceInitialHost()).withCredentials(credentials).build();
	}
}
