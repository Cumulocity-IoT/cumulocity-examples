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

import com.cumulocity.agent.snmp.bootstrap.model.BootstrapReadyEvent;
import com.cumulocity.agent.snmp.bootstrap.model.CredentialsAvailableEvent;
import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.agent.snmp.platform.model.GatewayManagedObjectWrapper;
import com.cumulocity.agent.snmp.platform.model.PlatformConnectionReadyEvent;
import com.cumulocity.agent.snmp.platform.service.GatewayDataProvider;
import com.cumulocity.agent.snmp.platform.service.PlatformProvider;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.devicebootstrap.DeviceCredentialsRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Permission;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BootstrapServiceTest {

	@Mock
	private IdentityApi identityApi;

	@Mock
	private InventoryApi inventoryApi;

	@Mock
	private Platform bootstrapPlatform;

	@Mock
	private TaskScheduler taskScheduler;

	@Mock
	private GatewayProperties properties;

	@Mock
	private PlatformProvider platformProvider;

	@Mock
	private GatewayDataProvider gatewayDataProvider;

	@Mock
	private DeviceCredentialsStoreService dataStore;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private DeviceCredentialsApi deviceCredentialsApi;

	@InjectMocks
	private BootstrapService bootstrapService;

	@Test
	public void shouldScheduleFixedDelayTaskForPollingDeviceCredentials() {
		when(properties.getBootstrapFixedDelay()).thenReturn(10000);

		bootstrapService.afterPropertiesSet();

		verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(10000L));
	}

	@Test
	public void shouldNotTryToPollDeviceCredentialsIfPresentLocally() {
		ArgumentCaptor<Runnable> pollingDeviceCredentialsCaptor = ArgumentCaptor.forClass(Runnable.class);
		DeviceCredentialsRepresentation deviceCredentials = new DeviceCredentialsRepresentation();

		when(properties.getBootstrapFixedDelay()).thenReturn(10000);
		when(dataStore.fetch()).thenReturn(deviceCredentials);

		bootstrapService.afterPropertiesSet();

		verify(taskScheduler).scheduleWithFixedDelay(pollingDeviceCredentialsCaptor.capture(), eq(10000L));
		pollingDeviceCredentialsCaptor.getValue().run();
		verifyNoInteractions(platformProvider);
		verify(eventPublisher).publishEvent(any(CredentialsAvailableEvent.class));
	}

	@Test
	public void shouldPollDeviceCredentialsIfNotAvailableLocally() {
		ArgumentCaptor<Runnable> pollingDeviceCredentialsCaptor = ArgumentCaptor.forClass(Runnable.class);
		DeviceCredentialsRepresentation deviceCredentials = new DeviceCredentialsRepresentation();
		deviceCredentials.setTenantId("test");
		deviceCredentials.setUsername("user");
		deviceCredentials.setPassword("password");

		when(dataStore.fetch()).thenReturn(null);
		when(properties.getBootstrapFixedDelay()).thenReturn(10000);
		when(properties.getGatewayIdentifier()).thenReturn("gateway#1");
		when(platformProvider.getBootstrapPlatform()).thenReturn(bootstrapPlatform);
		when(bootstrapPlatform.getDeviceCredentialsApi()).thenReturn(deviceCredentialsApi);
		when(deviceCredentialsApi.pollCredentials("gateway#1")).thenReturn(deviceCredentials);

		bootstrapService.afterPropertiesSet();

		verify(taskScheduler).scheduleWithFixedDelay(pollingDeviceCredentialsCaptor.capture(), eq(10000L));
		pollingDeviceCredentialsCaptor.getValue().run();
		verify(platformProvider).getBootstrapPlatform();
		verify(deviceCredentialsApi).pollCredentials("gateway#1");
		verify(eventPublisher).publishEvent(any(CredentialsAvailableEvent.class));
		verify(dataStore).store(argThat(object -> "test".equals(deviceCredentials.getTenantId())
				&& "user".equals(deviceCredentials.getUsername())
				&& "password".equals(deviceCredentials.getPassword())));
	}

	@Test
	public void shouldPollCredentialsIfBootstrapIsForced() {
		ArgumentCaptor<Runnable> pollingDeviceCredentialsCaptor = ArgumentCaptor.forClass(Runnable.class);
		DeviceCredentialsRepresentation deviceCredentials = new DeviceCredentialsRepresentation();
		deviceCredentials.setTenantId("test");
		deviceCredentials.setUsername("user");
		deviceCredentials.setPassword("password");

		when(dataStore.fetch()).thenReturn(deviceCredentials);
		when(properties.getBootstrapFixedDelay()).thenReturn(10000);
		when(properties.isForcedBootstrap()).thenReturn(true);
		when(properties.getGatewayIdentifier()).thenReturn("gateway#1");
		when(platformProvider.getBootstrapPlatform()).thenReturn(bootstrapPlatform);
		when(bootstrapPlatform.getDeviceCredentialsApi()).thenReturn(deviceCredentialsApi);
		when(deviceCredentialsApi.pollCredentials("gateway#1")).thenReturn(deviceCredentials);

		bootstrapService.afterPropertiesSet();

		verify(taskScheduler).scheduleWithFixedDelay(pollingDeviceCredentialsCaptor.capture(), eq(10000L));
		pollingDeviceCredentialsCaptor.getValue().run();
		verify(platformProvider).getBootstrapPlatform();
		verify(deviceCredentialsApi).pollCredentials("gateway#1");
		verify(eventPublisher).publishEvent(any(CredentialsAvailableEvent.class));
		verify(dataStore).store(argThat(object -> "test".equals(deviceCredentials.getTenantId())
				&& "user".equals(deviceCredentials.getUsername())
				&& "password".equals(deviceCredentials.getPassword())));
	}

	@Test
	public void shouldCreateGatewayDeviceIfNotPresentInPlatform() {
		PlatformConnectionReadyEvent event = new PlatformConnectionReadyEvent();
		SDKException exception = new SDKException(HttpStatus.SC_NOT_FOUND, "Test SDK Exception");

		when(properties.getGatewayIdentifier()).thenReturn("gateway#1");
		when(properties.getGatewayAvailabilityInterval()).thenReturn(10);
		when(identityApi.getExternalId(any())).thenThrow(exception);
		when(inventoryApi.create(any(ManagedObjectRepresentation.class)))
				.thenAnswer((Answer<ManagedObjectRepresentation>) invocation -> {
					Object[] args = invocation.getArguments();
					ManagedObjectRepresentation gatewayMo = (ManagedObjectRepresentation) args[0];
					GId id = new GId("123");
					gatewayMo.setId(id);
					return gatewayMo;
				});

		ReflectionTestUtils.invokeMethod(bootstrapService, "createDeviceIfNotExist", event);

		verify(identityApi, times(1)).getExternalId(any());
		verify(inventoryApi, times(1)).create(any());
		verify(identityApi, times(1)).create(any());
		verify(eventPublisher).publishEvent(any(BootstrapReadyEvent.class));
		verify(identityApi).create(argThat(extID -> extID.getExternalId().equals(properties.getGatewayIdentifier())
				&& extID.getType().equals(GatewayManagedObjectWrapper.C8Y_EXTERNAL_ID_TYPE)
				&& extID.getManagedObject().getName().equals(properties.getGatewayIdentifier())));
	}

	@Test
	public void shouldSkipGatewayDeviceCreationIfAlreadyCreated() {
		PlatformConnectionReadyEvent event = new PlatformConnectionReadyEvent("device_SnmpTest");
		ManagedObjectRepresentation deviceMO = new ManagedObjectRepresentation();
		deviceMO.setName("gateway#1");
		deviceMO.setType(GatewayManagedObjectWrapper.C8Y_SNMP_GATEWAY_TYPE);
		deviceMO.setId(new GId("123"));
		deviceMO.setOwner("device_SnmpTest");

		ExternalIDRepresentation externalId = new ExternalIDRepresentation();
		externalId.setExternalId("gateway#1");
		externalId.setType(GatewayManagedObjectWrapper.C8Y_EXTERNAL_ID_TYPE);
		externalId.setManagedObject(deviceMO);

		when(properties.getGatewayIdentifier()).thenReturn("gateway#1");
		when(identityApi.getExternalId(any())).thenReturn(externalId);
		when(inventoryApi.get(deviceMO.getId())).thenReturn(deviceMO);

		ReflectionTestUtils.invokeMethod(bootstrapService, "createDeviceIfNotExist", event);

		verify(inventoryApi, times(1)).get(deviceMO.getId());
		verify(eventPublisher).publishEvent(any(BootstrapReadyEvent.class));
	}

	@Test
	public void shouldUpdateGatewayObjectOnPlatformConnectionReadyEvent() {
		PlatformConnectionReadyEvent event = new PlatformConnectionReadyEvent("device_SnmpTest");
		ManagedObjectRepresentation deviceMO = new ManagedObjectRepresentation();
		deviceMO.setName("gateway#1");
		deviceMO.setType(GatewayManagedObjectWrapper.C8Y_SNMP_GATEWAY_TYPE);
		deviceMO.setId(new GId("123"));
		deviceMO.setOwner("device_SnmpTest");

		ExternalIDRepresentation externalId = new ExternalIDRepresentation();
		externalId.setExternalId("gateway#1");
		externalId.setType(GatewayManagedObjectWrapper.C8Y_EXTERNAL_ID_TYPE);
		externalId.setManagedObject(deviceMO);

		when(properties.getGatewayIdentifier()).thenReturn("gateway#1");
		when(identityApi.getExternalId(any())).thenReturn(externalId);
		when(inventoryApi.get(deviceMO.getId())).thenReturn(deviceMO);

		ReflectionTestUtils.invokeMethod(bootstrapService, "createDeviceIfNotExist", event);

		verify(gatewayDataProvider).updateGatewayObjects(any());
	}

	@Test
	public void shouldExitProcessIfGatewayDeviceIsCreatedByOtherUser() {
		System.setSecurityManager(new NoExitSecurityManager());
		PlatformConnectionReadyEvent event = new PlatformConnectionReadyEvent("device_SnmpTest");

		ManagedObjectRepresentation deviceMO = new ManagedObjectRepresentation();
		deviceMO.setName("gateway#1");
		deviceMO.setType(GatewayManagedObjectWrapper.C8Y_SNMP_GATEWAY_TYPE);
		deviceMO.setId(new GId("123"));
		deviceMO.setOwner("test-user");

		ExternalIDRepresentation externalId = new ExternalIDRepresentation();
		externalId.setExternalId("gateway#1");
		externalId.setType(GatewayManagedObjectWrapper.C8Y_EXTERNAL_ID_TYPE);
		externalId.setManagedObject(deviceMO);

		when(properties.getGatewayIdentifier()).thenReturn("gateway#1");
		when(identityApi.getExternalId(any())).thenReturn(externalId);
		when(inventoryApi.get(deviceMO.getId())).thenReturn(deviceMO);

		try {
			ReflectionTestUtils.invokeMethod(bootstrapService, "createDeviceIfNotExist", event);
		} catch (ExitException e) {
			assertEquals("Process exit test", 0, e.status);
		} finally {
			System.setSecurityManager(null);
		}
	}

	@Test
	public void shouldExitProcessForInvalidGatewayDeviceCredentials() {
		System.setSecurityManager(new NoExitSecurityManager());
		PlatformConnectionReadyEvent event = new PlatformConnectionReadyEvent("device_WrongDevice");
		BeanCreationException parentException = new BeanCreationException("Invalid device credentials");
		SDKException sdkException = new SDKException(HttpStatus.SC_UNAUTHORIZED, "Invalid device credentials");
		parentException.initCause(sdkException);

		when(properties.getGatewayIdentifier()).thenReturn("gateway#1");
		when(identityApi.getExternalId(any())).thenThrow(parentException);

		try {
			ReflectionTestUtils.invokeMethod(bootstrapService, "createDeviceIfNotExist", event);
		} catch (ExitException e) {
			assertEquals("Process exit test", 0, e.status);
		} finally {
			System.setSecurityManager(null);
		}
	}

	private static class NoExitSecurityManager extends SecurityManager {
		@Override
		public void checkPermission(Permission perm) {
			// allow anything.
		}

		@Override
		public void checkPermission(Permission perm, Object context) {
			// allow anything.
		}

		@Override
		public void checkExit(int status) {
			super.checkExit(status);
			throw new ExitException(status);
		}
	}

	@SuppressWarnings("serial")
	private static class ExitException extends SecurityException {

		int status;

		public ExitException(int status) {
			super("Exit test Exception!");
			this.status = status;
		}
	}
}
