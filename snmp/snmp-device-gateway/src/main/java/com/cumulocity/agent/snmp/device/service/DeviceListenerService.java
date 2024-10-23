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

package com.cumulocity.agent.snmp.device.service;

import static com.cumulocity.agent.snmp.util.SnmpUtil.getAuthProtocolOid;
import static com.cumulocity.agent.snmp.util.SnmpUtil.getPrivacyProtocolOid;

import java.io.IOException;
import java.net.BindException;
import java.net.ProtocolException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import jakarta.annotation.PreDestroy;

import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.cumulocity.agent.snmp.bootstrap.model.BootstrapReadyEvent;
import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.agent.snmp.platform.model.DeviceManagedObjectWrapper;
import com.cumulocity.agent.snmp.platform.model.DeviceManagedObjectWrapper.DeviceAuthentication;
import com.cumulocity.agent.snmp.platform.model.DeviceManagedObjectWrapper.SnmpDeviceProperties;
import com.cumulocity.agent.snmp.platform.model.DeviceProtocolManagedObjectWrapper;
import com.cumulocity.agent.snmp.platform.model.GatewayDataRefreshedEvent;
import com.cumulocity.agent.snmp.platform.service.GatewayDataProvider;
import com.cumulocity.agent.snmp.util.IpAddressUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DeviceListenerService {

	@Autowired
	private GatewayProperties gatewayProperties;

	@Autowired
	private GatewayProperties.SnmpProperties snmpProperties;

	@Autowired
	private GatewayDataProvider gatewayDataProvider;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private DeviceDataHandler deviceDataHandler;

	Snmp snmp = null;

	private ScheduledFuture<?> snmpDevicePoller;

	private long pollingRateInSeconds = -1;

	@EventListener(BootstrapReadyEvent.class)
	private void onBootstrapReady() {
		createSnmpDeviceListener();

		createSnmpDevicePoller();
	}

	@EventListener(GatewayDataRefreshedEvent.class)
	private void onGatewayDataRefresh() {
		// Re-Configure user security model
		configureUserSecurityModel();

		// Re-schedule poller (only if configuration changed
		createSnmpDevicePoller();
	}

	protected void createSnmpDeviceListener() {
		try {
			configureUserSecurityModel();

			int poolSize = gatewayProperties.getThreadPoolSizeForTrapProcessing();
			ThreadPool threadPool = ThreadPool.create("snmp-dispatcher-thread", poolSize);
			MessageDispatcher messageDispatcher = new MessageDispatcherImpl();

			MultiThreadedMessageDispatcher dispatcher = new MultiThreadedMessageDispatcher(threadPool,
					messageDispatcher);
			dispatcher.addMessageProcessingModel(new MPv1());
			dispatcher.addMessageProcessingModel(new MPv2c());
			dispatcher.addMessageProcessingModel(new MPv3());

			String trapListenerBindingAddress = IpAddressUtil.sanitizeIpAddress(snmpProperties.getTrapListenerAddress(),
					true);
			TransportMapping<? extends Address> transportMapping = createTransportMapping(
					snmpProperties.getTrapListenerProtocol(), snmpProperties.getTrapListenerPort(),
					trapListenerBindingAddress);

			snmp = new Snmp(dispatcher, transportMapping);
			snmp.addCommandResponder(deviceDataHandler);
			snmp.listen();

			log.info("Started listening to traps at {}", transportMapping.getListenAddress().toString());
		} catch (BindException be) {
			log.error("Failed to start listening to traps. Port {}/{} is already in use.\n"
					+ "Update the 'snmp.trapListener.port' and 'snmp.trapListener.address' properties in file:${user.home}/.snmp/snmp-agent-gateway.properties and restart the agent. "
					+ "Shutting down the agent...", snmpProperties.getTrapListenerAddress(),
					snmpProperties.getTrapListenerPort(), be);
			System.exit(0);
		} catch (IOException | IllegalArgumentException t) {
			log.error("Failed to start listening to traps on port {}/{}.\n"
					+ "Update the 'snmp.trapListener.port' and 'snmp.trapListener.address' properties in file:${user.home}/.snmp/snmp-agent-gateway.properties and restart the agent. "
					+ "Shutting down the agent...", snmpProperties.getTrapListenerAddress(),
					snmpProperties.getTrapListenerPort(), t);
			System.exit(0);
		}
	}

	protected void createSnmpDevicePoller() {
		long newPollingRateInSeconds = gatewayDataProvider.getGatewayDevice().getSnmpCommunicationProperties()
				.getPollingRate();

		if (snmpDevicePoller != null && !snmpDevicePoller.isDone() && pollingRateInSeconds == newPollingRateInSeconds) {
			return;
		}

		if (snmpDevicePoller != null) {
			snmpDevicePoller.cancel(true);
		}

		pollingRateInSeconds = newPollingRateInSeconds;

		if (pollingRateInSeconds <= 0) {
			return;
		}
		snmpDevicePoller = taskScheduler.scheduleWithFixedDelay(() -> {
			try {
				Map<String, DeviceManagedObjectWrapper> snmpDeviceMap = gatewayDataProvider.getSnmpDeviceMap();
				Map<String, DeviceProtocolManagedObjectWrapper> protocolMap = gatewayDataProvider.getProtocolMap();

				snmpDeviceMap.forEach((deviceIp, deviceWrapper) -> {
					String deviceProtocolName = deviceWrapper.getDeviceProtocol();

					if (protocolMap.containsKey(deviceProtocolName)) {
						DeviceProtocolManagedObjectWrapper deviceProtocolWrapper = protocolMap.get(deviceProtocolName);
						List<VariableBinding> varibleBindingList = deviceProtocolWrapper
								.getMeasurementVariableBindingList();

						if (varibleBindingList.size() > 0) {

							taskScheduler.schedule(() -> {
								SnmpDevicePoller devicePoller = null;

								try {
									devicePoller = new SnmpDevicePoller(snmpProperties, deviceWrapper,
											varibleBindingList);
									ResponseEvent responseEvent = devicePoller.poll();

									PDU responsePDU = responseEvent.getResponse();
									if (responsePDU == null) {
										log.error("Empty response was received while polling for device with IP Address {} and OIDs {}",
												deviceIp, responseEvent.getRequest());
									} else if (responsePDU.getErrorStatus() == PDU.noError) {
										if (responsePDU.getVariableBindings().size() == 0) {
											log.error("No data found after successful device polling");
											return;
										}

										deviceDataHandler.processDevicePdu(deviceIp, responsePDU);
									} else {
										log.error(
												"Error while polling device {} OIDs.\n"
														+ "Error index {} | Error status {} | Error text {} ",
												deviceIp, responsePDU.getErrorIndex(), responsePDU.getErrorStatus(),
												responsePDU.getErrorStatusText());
									}

								} catch (IOException e) {
									log.error("Failed while polling variables for the device with IP Address {} and {} protocol",
											deviceIp, deviceWrapper.getDeviceProtocol(), e);
								} finally {
									if (devicePoller != null) {
										devicePoller.close();
									}
								}
							}, Instant.now());

						}
					} else {
						log.debug("{} device is not configured with device protocol", deviceIp);
					}
				});
			} catch (Throwable t) {
				log.error("Error while polling SNMP devices.", t);
			}
		}, Duration.ofSeconds(pollingRateInSeconds));
	}

	@PreDestroy
	private void stop() {
		try {
			if (snmp != null) {
				snmp.close();
			}
		} catch (IOException ex) {
			log.error("Failed to stop trap listener", ex);
		}
	}

	private void configureUserSecurityModel() {
		SecurityProtocols securityProtocols = SecurityProtocols.getInstance();
		securityProtocols.addDefaultProtocols();

		USM usm = new USM(securityProtocols, new OctetString(MPv3.createLocalEngineID()), 0);
		addCredentials(usm);

		SecurityModels.getInstance().addSecurityModel(usm);
	}

	private void addCredentials(USM usm) {
		Map<String, DeviceManagedObjectWrapper> snmpDeviceMap = gatewayDataProvider.getSnmpDeviceMap();
		snmpDeviceMap.forEach((deviceIP, managedObject) -> {
			SnmpDeviceProperties properties = managedObject.getProperties();

			if (properties.getVersion() == SnmpConstants.version3) {
				DeviceAuthentication authDetails = properties.getAuth();
				UsmUser usmUser = createUser(authDetails);

				if (usmUser != null) {
					OctetString userName = new OctetString(authDetails.getUsername());
					OctetString engineID = new OctetString(authDetails.getEngineId());
					usm.addUser(userName, engineID, usmUser);
				}
			}
		});
	}

	private UsmUser createUser(DeviceAuthentication authDetails) {
		OctetString userName = new OctetString(authDetails.getUsername());

		switch (authDetails.getSecurityLevel()) {
		case SecurityLevel.NOAUTH_NOPRIV:
			return new UsmUser(userName, null, null, null, null);

		case SecurityLevel.AUTH_NOPRIV:
			OID authProtocolOid = getAuthProtocolOid(authDetails.getAuthProtocol());
			OctetString authPassword = new OctetString(authDetails.getAuthPassword());
			return new UsmUser(userName, authProtocolOid, authPassword, null, null);

		case SecurityLevel.AUTH_PRIV:
			authProtocolOid = getAuthProtocolOid(authDetails.getAuthProtocol());
			authPassword = new OctetString(authDetails.getAuthPassword());
			OID privacyProtocolOid = getPrivacyProtocolOid(authDetails.getPrivProtocol());
			OctetString privacyPassword = new OctetString(authDetails.getPrivPassword());
			return new UsmUser(userName, authProtocolOid, authPassword, privacyProtocolOid, privacyPassword);

		default:
			log.error("Unsupported {} Security level found in {} user, configured for device having {} as engine id",
					authDetails.getSecurityLevel(), userName, authDetails.getEngineId());
			return null;
		}
	}

	TransportMapping<? extends Address> createTransportMapping(String protocol, int port, String bindingAddress)
			throws IOException {
		Address snmpListeningAddress = GenericAddress.parse(protocol + ":" + bindingAddress + "/" + port);

		if (snmpListeningAddress instanceof TcpAddress) {
			return new DefaultTcpTransportMapping((TcpAddress) snmpListeningAddress);
		} else if (snmpListeningAddress instanceof UdpAddress) {
			return new DefaultUdpTransportMapping((UdpAddress) snmpListeningAddress);
		} else {
			String msg = "Unable to service snmp devices. Unsupported " + protocol + " protocol selected. "
					+ "Currently supported protocols are TCP and UDP.";
			log.error(msg);

			throw new ProtocolException(msg);
		}
	}
}
