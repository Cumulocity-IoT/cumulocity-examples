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

import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.rest.representation.tenant.TenantRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.RestOperations;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.MediaType;

import static org.junit.Assert.assertNotNull;

@Slf4j
@Component
public class TenantProvider {

	@Autowired
	private PlatformProvider platformProvider;

	@Getter
	private TenantRepresentation testTenant;

	public void createIntegrationTestTenant() {
		String randomStr = Long.toString(System.currentTimeMillis());
		String tenantName = String.join("-", "cucumber", "tenant", randomStr);
		Platform managementPlatform = platformProvider.switchToManagement();
		RestOperations rest = managementPlatform.rest();
		TenantRepresentation tenant = new TenantRepresentation();
		tenant.setCompany("Cumulocity SNMP Gateway");
		tenant.setDomain(tenantName + ".cumulocity.com");
		tenant.setAdminName("admin");
		tenant.setAdminPass("passw0rd_a");
		tenant.setId(tenantName);

		log.info("Creating integration test tenant: " + tenantName);
		testTenant = rest.post("/tenant/tenants", MediaType.APPLICATION_JSON_TYPE, tenant);
		assertNotNull(testTenant);
		assertNotNull(testTenant.getId());
		log.info("Tenant created with ID: " + testTenant.getId());
	}

	public void deleteIntegrationTestTenant() {
		Platform managementPlatform = platformProvider.switchToManagement();
		RestOperations rest = managementPlatform.rest();
		log.info("Deleting cucumber tenant: " + testTenant.getId());
		rest.delete("/tenant/tenants/" + testTenant.getId());
		log.info("Tenant deleted");
	}

	public CumulocityBasicCredentials getTestTenantCredentials() {
		return CumulocityBasicCredentials.builder().tenantId(testTenant.getId()).username("admin")
				.password("passw0rd_a").build();
	}
}