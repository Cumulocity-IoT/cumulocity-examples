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

package com.cumulocity.agent.snmp.platform.service;

import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.agent.snmp.platform.model.DeviceManagedObjectWrapper;
import com.cumulocity.agent.snmp.platform.model.GatewayDataRefreshedEvent;
import com.cumulocity.agent.snmp.platform.model.ReceivedOperationForGatewayEvent;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectReferenceCollectionRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectReferenceRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.notification.SubscriptionListener;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GatewayDataProviderTest {

	@Mock
	private InventoryApi inventoryApi;

	@Mock
	private DeviceControlApi deviceControlApi;

	@Captor
	private ArgumentCaptor<SubscriptionListener<GId, OperationRepresentation>> subscriptionListenerCaptor;

	@Captor
	private ArgumentCaptor<ReceivedOperationForGatewayEvent> receivedOperationForGatewayEventCaptor;

	@Mock
	private GatewayProperties properties;

	@Mock
	private PlatformProvider platformProvider;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Spy
	private ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

	@Spy
	@InjectMocks
	GatewayDataProvider gatewayDataProvider;

	@Before
	public void setup() {
		taskScheduler.initialize();
	}

	@Test
	public void shouldScheduleGatewayDataRefreshOnUpdateGatewayObjects() {
		ManagedObjectReferenceCollectionRepresentation childDevices = new ManagedObjectReferenceCollectionRepresentation();
		ManagedObjectRepresentation gatewayDeviceMo = new ManagedObjectRepresentation();
		gatewayDeviceMo.setId(new GId("snmp-agent"));
		gatewayDeviceMo.setChildDevices(childDevices);

		when(properties.getGatewayObjectRefreshIntervalInMinutes()).thenReturn(2);
		when(inventoryApi.get(any())).thenReturn(gatewayDeviceMo);

		// Action
		gatewayDataProvider.updateGatewayObjects(gatewayDeviceMo);

		verify(gatewayDataProvider, times(1)).scheduleGatewayDataRefresh();
		verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofMinutes(2)));
	}

	@Test
	public void shouldRefreshGatewayObjectsOnUpdateGatewayObjects() {
		ManagedObjectRepresentation gatewayDeviceMo = new ManagedObjectRepresentation();
		gatewayDeviceMo.setId(new GId("snmp-agent"));

		ManagedObjectRepresentation childDeviceMo = new ManagedObjectRepresentation();
		childDeviceMo.setId(new GId("child-device"));

		ManagedObjectReferenceCollectionRepresentation childDevices = new ManagedObjectReferenceCollectionRepresentation();
		ManagedObjectReferenceRepresentation childDeviceRef = new ManagedObjectReferenceRepresentation();
		childDeviceRef.setManagedObject(childDeviceMo);

		Map<String, Object> propertiesMap = new HashMap<>();
		propertiesMap.put("version", 0);
		propertiesMap.put("port", "161");
		propertiesMap.put("type", "/inventory/managedObjects/device-protocol");
		propertiesMap.put("ipAddress", "127.0.0.1");

		childDeviceMo.set(propertiesMap, DeviceManagedObjectWrapper.C8Y_SNMP_DEVICE);

		List<ManagedObjectReferenceRepresentation> childDeviceRefList = new ArrayList<>();
		childDeviceRefList.add(childDeviceRef);
		childDevices.setReferences(childDeviceRefList);

		gatewayDeviceMo.setChildDevices(childDevices);

		ManagedObjectRepresentation deviceProtocolMo = new ManagedObjectRepresentation();
		deviceProtocolMo.setId(new GId("device-protocol"));

		when(inventoryApi.get(new GId("snmp-agent"))).thenReturn(gatewayDeviceMo);
		when(inventoryApi.get(new GId("child-device"))).thenReturn(childDeviceMo);
		when(inventoryApi.get(new GId("device-protocol"))).thenReturn(deviceProtocolMo);
		when(properties.getGatewayObjectRefreshIntervalInMinutes()).thenReturn(1);

		assertNull(gatewayDataProvider.getGatewayDevice());
		assertEquals(gatewayDataProvider.getSnmpDeviceMap().size(), 0);

		// Action
		gatewayDataProvider.updateGatewayObjects(gatewayDeviceMo);

		assertNotNull(gatewayDataProvider.getGatewayDevice());
		assertEquals(gatewayDataProvider.getSnmpDeviceMap().size(), 1);
	}

	@Test
	public void shouldNotUpdateDeviceProtocolIfNotPresentInPlatform() {
		ManagedObjectRepresentation gatewayDeviceMo = new ManagedObjectRepresentation();
		gatewayDeviceMo.setId(new GId("snmp-agent"));

		ManagedObjectRepresentation childDeviceMo = new ManagedObjectRepresentation();
		childDeviceMo.setId(new GId("child-device"));

		ManagedObjectReferenceCollectionRepresentation childDevices = new ManagedObjectReferenceCollectionRepresentation();
		ManagedObjectReferenceRepresentation childDeviceRef = new ManagedObjectReferenceRepresentation();
		childDeviceRef.setManagedObject(childDeviceMo);

		Map<String, Object> propertiesMap = new HashMap<>();
		propertiesMap.put("version", 0);
		propertiesMap.put("port", "161");
		propertiesMap.put("type", "/inventory/managedObjects/device-protocol");
		propertiesMap.put("ipAddress", "127.0.0.1");

		childDeviceMo.set(propertiesMap, DeviceManagedObjectWrapper.C8Y_SNMP_DEVICE);

		List<ManagedObjectReferenceRepresentation> childDeviceRefList = new ArrayList<>();
		childDeviceRefList.add(childDeviceRef);
		childDevices.setReferences(childDeviceRefList);

		gatewayDeviceMo.setChildDevices(childDevices);

		when(inventoryApi.get(new GId("snmp-agent"))).thenReturn(gatewayDeviceMo);
		when(inventoryApi.get(new GId("child-device"))).thenReturn(childDeviceMo);
		when(inventoryApi.get(new GId("device-protocol")))
				.thenThrow(new SDKException(HttpStatus.SC_NOT_FOUND, "Object Not found"));
		when(properties.getGatewayObjectRefreshIntervalInMinutes()).thenReturn(1);

		assertNull(gatewayDataProvider.getGatewayDevice());
		assertEquals(gatewayDataProvider.getSnmpDeviceMap().size(), 0);

		// Action
		gatewayDataProvider.updateGatewayObjects(gatewayDeviceMo);

		assertEquals(1, gatewayDataProvider.getSnmpDeviceMap().size());
		assertNull(gatewayDataProvider.getProtocolMap().get("device-protocol"));
	}

	@Test
	public void shouldGenerateGatewayDataRefreshedEventOnRefreshGatewayObjects() throws InterruptedException {
		ManagedObjectRepresentation gatewayDeviceMo = new ManagedObjectRepresentation();
		gatewayDeviceMo.setId(new GId("snmp-agent"));

		ManagedObjectReferenceCollectionRepresentation childDevices = new ManagedObjectReferenceCollectionRepresentation();
		ManagedObjectReferenceRepresentation childDeviceRef = new ManagedObjectReferenceRepresentation();
		ManagedObjectRepresentation childDeviceMo = new ManagedObjectRepresentation();
		childDeviceMo.setId(new GId("child-device"));

		childDeviceRef.setManagedObject(childDeviceMo);
		gatewayDeviceMo.setChildDevices(childDevices);

		when(properties.getGatewayObjectRefreshIntervalInMinutes()).thenReturn(1);
		when(platformProvider.isPlatformAvailable()).thenReturn(true);

		doAnswer((Answer<Void>) invocation -> {
			return null;
		}).when(gatewayDataProvider).refreshGatewayObjects();

		// Action
		gatewayDataProvider.scheduleGatewayDataRefresh();
		Thread.sleep(1000);

		verify(gatewayDataProvider).refreshGatewayObjects();
		verify(eventPublisher).publishEvent(any(GatewayDataRefreshedEvent.class));
	}
}
