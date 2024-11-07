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

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import jakarta.validation.constraints.Pattern;

@Data
@Configuration
@PropertySource(value = "file:${user.home}/.snmp/cucumber.properties", ignoreResourceNotFound = true)
public class GatewayIntegrationTestProperties {

	private TenantProperties mgmtTenantProperties;

	@Value("#{'${gateway.identifier}'.trim()}")
	@Pattern(regexp = "(.|\\s)*\\S(.|\\s)*")
	private String gatewayIdentifier;

	@Value("#{'${C8Y.baseURL:http://developers.cumulocity.com}'.trim()}")
	@Pattern(regexp = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)")
	private String baseUrl;

	@Value("#{'${C8Y.forceInitialHost:true}'.trim()}")
	private boolean forceInitialHost;

	@Value("#{'${gateway.jar.location:systemProperties['user.home']/.snmp/snmp-agent-gateway.jar}'.trim()}")
	private String gatewayJarLocation;

	@Configuration
	@Data
	@ToString(exclude = "password")
	public class TenantProperties {

		@Value("#{'${C8Y.tenant}'.trim()}")
		private String tenantId;

		@Value("#{'${C8Y.user}'.trim()}")
		private String username;

		@Value("#{'${C8Y.password}'.trim()}")
		private String password;
	}
}