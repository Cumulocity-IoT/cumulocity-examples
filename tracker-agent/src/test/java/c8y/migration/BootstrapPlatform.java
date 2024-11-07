/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.migration;

import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.sdk.client.PlatformImpl;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class BootstrapPlatform {

	private final Settings settings;
	private DeviceCredentialsApi deviceCredentialsApi;

	@Autowired
	public BootstrapPlatform(Settings settings) {
		this.settings = settings;
	}

	@PostConstruct
	public void init() {
		CumulocityBasicCredentials credentials = CumulocityBasicCredentials.builder()
		.tenantId(settings.getBootstrapTenant())
		.username(settings.getBootstrapUser())
		.password(settings.getBootstrapPassword())
		.build();
		PlatformImpl platform = new PlatformImpl(settings.getC8yHost(), credentials);
		deviceCredentialsApi = platform.getDeviceCredentialsApi();
	}

	public DeviceCredentialsApi getDeviceCredentialsApi() {
		return deviceCredentialsApi;
	}

}
