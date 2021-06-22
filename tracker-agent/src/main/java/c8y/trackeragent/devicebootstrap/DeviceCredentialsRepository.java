/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent.devicebootstrap;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import c8y.trackeragent.configuration.ConfigUtils;
import c8y.trackeragent.exception.UnknownDeviceException;
import c8y.trackeragent.utils.GroupPropertyAccessor;
import c8y.trackeragent.utils.GroupPropertyAccessor.Group;

@Component
public class DeviceCredentialsRepository {

    private static final String TENANT_ENTRY_PREFIX = "tenant-";

	private static final Logger logger = LoggerFactory.getLogger(DeviceCredentialsRepository.class);

    private final Map<String, DeviceCredentials> imei2DeviceCredentials = new ConcurrentHashMap<String, DeviceCredentials>();
    private final GroupPropertyAccessor devicePropertyAccessor;
    private final Object lock = new Object();
	private final String devicePropertiesPath;


    public DeviceCredentialsRepository(String devicePropertiesPath) {
    	this.devicePropertiesPath = devicePropertiesPath;
		devicePropertyAccessor = new GroupPropertyAccessor(devicePropertiesPath, asList("tenantId"));
    }
    public DeviceCredentialsRepository() {
    	this(ConfigUtils.get().getConfigFilePath(ConfigUtils.DEVICES_FILE_NAME));
    }

    public boolean hasDeviceCredentials(String imei) {
        return imei2DeviceCredentials.containsKey(imei);
    }
    
	public DeviceCredentials getDeviceCredentials(String imei) {
        DeviceCredentials result = imei2DeviceCredentials.get(imei);
        if (result == null) {
            throw UnknownDeviceException.forImei(imei);
        }
        return result.duplicate();
    }

    public List<DeviceCredentials> getAllDeviceCredentials() {
        return new ArrayList<DeviceCredentials>(imei2DeviceCredentials.values());
    }

	public void saveDeviceCredentials(DeviceCredentials newCredentials) {
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
        imei2DeviceCredentials.clear();
        for (Group group : devicePropertyAccessor.getGroups()) {
            if (group.isFullyInitialized()) {
                imei2DeviceCredentials.put(group.getName(), asDeviceCredentials(group));
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

	public List<DeviceCredentials> getAllDeviceCredentials(String tenant) {
		return from(getAllDeviceCredentials()).filter(DeviceCredentials.hasTenant(tenant)).toList();
	}
}