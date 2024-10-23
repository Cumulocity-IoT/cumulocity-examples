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

package com.cumulocity.agent.snmp.bootstrap.service;

import com.cumulocity.agent.snmp.bootstrap.model.DeviceCredentialsKey;
import com.cumulocity.agent.snmp.bootstrap.repository.DeviceCredentialsStore;
import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.model.JSONBase;
import com.cumulocity.rest.representation.devicebootstrap.DeviceCredentialsRepresentation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeviceCredentialsStoreService {

    private final GatewayProperties gatewayProperties;

    private final DeviceCredentialsStore deviceCredentialsStore;


    void store(DeviceCredentialsRepresentation credentials) {
        if(credentials == null) {
            throw new NullPointerException("credentials");
        }

        deviceCredentialsStore.put(createDeviceCredentialsKey(), credentials.toJSON());
    }

    DeviceCredentialsRepresentation fetch() {
        String deviceCredentialsJson = deviceCredentialsStore.get(createDeviceCredentialsKey());
        if(deviceCredentialsJson != null) {
            return JSONBase.fromJSON(deviceCredentialsJson, DeviceCredentialsRepresentation.class);
        }

        return null;
    }

    DeviceCredentialsRepresentation remove() {
        String deviceCredentialsJson = deviceCredentialsStore.remove(createDeviceCredentialsKey());

        if(deviceCredentialsJson != null) {
            return JSONBase.fromJSON(deviceCredentialsJson, DeviceCredentialsRepresentation.class);
        }

        return null;
    }

    @PreDestroy
    void closeDeviceCredentialsStore() {
        deviceCredentialsStore.close();
    }

    private DeviceCredentialsKey createDeviceCredentialsKey() {
        return new DeviceCredentialsKey(
                gatewayProperties.getBaseUrl(),
                gatewayProperties.getBootstrapProperties().getTenantId(),
                gatewayProperties.getBootstrapProperties().getUsername());
    }
}
