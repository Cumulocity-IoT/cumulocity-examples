/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent.devicebootstrap;

import c8y.trackeragent.configuration.ConfigUtils;
import c8y.trackeragent.utils.GroupPropertyAccessor;
import c8y.trackeragent.utils.GroupPropertyAccessor.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;

@Component
public class DeviceCredentialsRepository {

    private static final String TENANT_ENTRY_PREFIX = "tenant-";

	private static final Logger logger = LoggerFactory.getLogger(DeviceCredentialsRepository.class);

    private final Map<String, DeviceCredentials> imei2DeviceCredentials = new ConcurrentHashMap<String, DeviceCredentials>();
    private final Set<String> tenants = new HashSet<>();
    private final GroupPropertyAccessor devicePropertyAccessor;
    private final GroupPropertyAccessor agentPropertyAccessor;
    private final Object lock = new Object();
	private final String devicePropertiesPath;


    public DeviceCredentialsRepository(String devicePropertiesPath) {
    	this.devicePropertiesPath = devicePropertiesPath;
		devicePropertyAccessor = new GroupPropertyAccessor(devicePropertiesPath, asList("tenantId"));
    	agentPropertyAccessor = new GroupPropertyAccessor(devicePropertiesPath, asList("user", "password"));
    }
    public DeviceCredentialsRepository() {
    	this(ConfigUtils.get().getConfigFilePath(ConfigUtils.DEVICES_FILE_NAME));
    }

    public List<DeviceCredentials> getAllDeviceCredentials() {
        return new ArrayList<DeviceCredentials>(imei2DeviceCredentials.values());
    }

	public Set<String> getAllTenants() {
		return tenants;
	}

	/* only for tests */
	void saveDeviceCredentials(DeviceCredentials newCredentials) {
        synchronized (lock) {
            Group group = asDeviceGroup(newCredentials.getImei(), newCredentials);
            if (!group.isFullyInitialized()) {
                throw new IllegalArgumentException("Not fully initialized credentials: " + newCredentials);
            }
            devicePropertyAccessor.write(group);
            imei2DeviceCredentials.put(newCredentials.getImei(), newCredentials);
            logger.info("Credentials for device {} have been written: {}.", newCredentials.getImei(), newCredentials);
        }
    }

    @PostConstruct
    public void refresh() throws IOException {
    	File deviceProperties = new File(devicePropertiesPath);
    	if (!deviceProperties.exists()) {
    		deviceProperties.createNewFile();
    	}
        devicePropertyAccessor.refresh();
        agentPropertyAccessor.refresh();
        imei2DeviceCredentials.clear();
        tenants.clear();
        for (Group group : devicePropertyAccessor.getGroups()) {
            if (group.isFullyInitialized()) {
                imei2DeviceCredentials.put(group.getName(), asDeviceCredentials(group));
            }
        }
        for (Group group : agentPropertyAccessor.getGroups()) {
        	if (group.isFullyInitialized()) {
        		String tenant = groupNameToTenant(group.getName());
        		tenants.add(tenant);
        	}
        }
    }

    private DeviceCredentials asDeviceCredentials(Group group) {
    	return DeviceCredentials.forDevice(group.getName(), group.get("tenantId"));
    }

    private Group asDeviceGroup(String imei, DeviceCredentials credentials) {
        Group group = devicePropertyAccessor.createEmptyGroup(imei);
        group.put("tenantId", credentials.getTenant());
        return group;
    }

    private static String groupNameToTenant(String groupName) {
    	return groupName.replaceFirst(TENANT_ENTRY_PREFIX, "");
    }
}