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

package com.cumulocity.agent.snmp.cucumber.steps;

import c8y.IsDevice;
import com.cumulocity.agent.snmp.cucumber.config.GatewayIntegrationTestProperties;
import com.cumulocity.agent.snmp.cucumber.config.PlatformProvider;
import com.cumulocity.agent.snmp.cucumber.config.TenantProvider;
import com.cumulocity.agent.snmp.cucumber.tools.GatewayLogger;
import com.cumulocity.agent.snmp.cucumber.tools.ProcessInstance;
import com.cumulocity.agent.snmp.cucumber.tools.TaskExecutor;
import com.cumulocity.model.Agent;
import com.cumulocity.model.ID;
import com.cumulocity.rest.representation.devicebootstrap.NewDeviceRequestCollectionRepresentation;
import com.cumulocity.rest.representation.devicebootstrap.NewDeviceRequestRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

@Slf4j
public class GatewayRegistration {

    @Autowired
    private GatewayLogger gatewayLogger;

    @Autowired
    private TenantProvider tenantProvider;

    @Autowired
    private PlatformProvider platformProvider;

    @Autowired
    private GatewayIntegrationTestProperties properties;

    private ProcessInstance gatewayProcess = null;

    @Getter
    private ManagedObjectRepresentation gatewayDevice;

    @Given("^I start and register gateway with (.+) protocol$")
    public void setupGatewayWithConfig(String trapListenerProtocol) throws IOException {
        setupGatewayWithConfig(161, trapListenerProtocol);
    }

    @Given("^I start and register gateway with (.+) protocol and polling port ([0-9]+)$")
    public void setupGatewayWithConfig(String trapListenerProtocol, Integer pollingPort) throws IOException {
        setupGatewayWithConfig(pollingPort, trapListenerProtocol);
    }

    private void setupGatewayWithConfig(Integer pollingPort, String trapListenerProtocol) throws IOException {
        createGatewayConfigurationFile(pollingPort, trapListenerProtocol);
        startGatewayProcess();
        createGatewayDevice();
        acceptGatewayDeviceRequest();
        gatewayDeviceShouldExist();
    }

    @Given("^There are no existing device credentials available locally$")
    public void deleteDeviceCredentials() throws IOException {
        log.info("Deleting device credentials if available");
        String deviceCredFilePath = getDeviceCredentialsFilePath();

        log.info("Deleting file: " + deviceCredFilePath);
        File deviceCredFile = new File(deviceCredFilePath);
        if (deviceCredFile.exists()) {
            FileUtils.forceDelete(new File(deviceCredFilePath));
        }

        assertFalse(new File(deviceCredFilePath).exists());

        log.info("Device credentials file deleted");
    }

    @When("^I start the gateway process$")
    public void startGatewayProcess() throws IOException {
        String jar = "-jar";
        String java = "java";
        String profile = "-Dspring.profiles.active=" + tenantProvider.getTestTenant().getId();

        log.info("Starting gateway process: {} {} {} {}", java, profile, jar, properties.getGatewayJarLocation());
        ProcessBuilder ps = new ProcessBuilder(java, profile, jar, properties.getGatewayJarLocation());
        gatewayProcess = new ProcessInstance();
        gatewayProcess.start(ps);
        gatewayLogger.attachToProcess(gatewayProcess.get());

        log.info("Gateway process successfully started!");
    }

    @And("^I create gateway configuration$")
    public void createGatewayConfigurationFile() throws IOException {
        createGatewayConfigurationFile(161, "UDP");
    }

    private void createGatewayConfigurationFile(Integer pollingPort, String trapListenerProtocol) throws IOException {
        log.info("Generating configuration for gateway... ");

        String config = replaceConfigurationParams(pollingPort, trapListenerProtocol);

        log.info("New configuration: \n{}", config);

        String configFilePath = getConfigFilePath();
        File configFile = new File(configFilePath);
        FileUtils.writeStringToFile(configFile, config);

        assertTrue(configFile.exists());

        log.info("Gateway configuration saved to {}", configFilePath);
    }

    private String replaceConfigurationParams(Integer pollingPort, String trapListenerProtocol) throws IOException {
        String config = IOUtils.toString(getClass().getResourceAsStream("/snmp-agent-gateway-template.properties"));
        config = config.replaceAll("\\{\\{C8Y.baseURL}}", properties.getBaseUrl())
                .replaceAll("\\{\\{test.tenant}}", tenantProvider.getTestTenant().getId())
                .replaceAll("\\{\\{C8Y.forceInitialHost}}", Boolean.toString(properties.isForceInitialHost()))
                .replaceAll("\\{\\{gateway.identifier}}", getSnmpGatewayDeviceName())
                .replaceAll("\\{\\{snmp.polling.port}}", Integer.toString(pollingPort))
                .replaceAll("\\{\\{snmp.trapListener.protocol}}", trapListenerProtocol);
        return config;
    }

    @And("^There is no gateway device present in the platform$")
    public void checkGatewayDeviceNotPresent() {
        boolean status = isGatewayDeviceExist();

        assertFalse(status);
    }

    @And("^I register the gateway device$")
    public void createGatewayDevice() {
        log.info("Registering new device...");

        Platform platform = platformProvider.createIntegrationTestPlatform(tenantProvider.getTestTenantCredentials());
        NewDeviceRequestRepresentation gatewayDeviceRequest = platform.getDeviceCredentialsApi().register(getSnmpGatewayDeviceName());

        log.info("New gateway device registered with ID: {}", gatewayDeviceRequest.getId());
    }

    @And("I accept gateway device request")
    public void acceptGatewayDeviceRequest() {
        log.info("Accepting new gateway device request");

        Platform platform = platformProvider.createIntegrationTestPlatform(tenantProvider.getTestTenantCredentials());
        final NewDeviceRequestRepresentation[] toUpdate = { null };
        if (!TaskExecutor.run(() -> {
            NewDeviceRequestCollectionRepresentation requestsCollection = platform.rest().get("/devicecontrol/newDeviceRequests",
                    MediaType.APPLICATION_JSON_TYPE, NewDeviceRequestCollectionRepresentation.class);
            List<NewDeviceRequestRepresentation> requestList = requestsCollection.getNewDeviceRequests();
            assertNotNull(requestList);
            assertEquals(1, requestList.size());
            if (requestList.get(0).getStatus().toLowerCase().matches(".*pending.*")) {
                toUpdate[0] = requestList.get(0);
                return true;
            }
            return false;
        })) {
            throw new RuntimeException("Couldn't find new device request in pending state within timeout!");
        }

        NewDeviceRequestRepresentation update = new NewDeviceRequestRepresentation();
        update.setStatus("ACCEPTED");
        String url = "/devicecontrol/newDeviceRequests/" + toUpdate[0].getId();
        platform.rest().put(url, MediaType.APPLICATION_JSON_TYPE, update);

        log.info("Accepted gateway device request");
    }

    @And("Gateway device is available")
    public void checkGatewayDeviceAvailability() {
        boolean status = isGatewayDeviceExist();

        assertTrue(status);

        log.info("Found device with ID: " + gatewayDevice.getId().getValue());
    }

    @And("^I create a snmp gateway device using (.+) user$")
    public void createSnmpDevice(String username) {

        ManagedObjectRepresentation deviceMO = new ManagedObjectRepresentation();
        deviceMO.setName("snmp-agent");
        deviceMO.setType("c8y_SNMP");
        deviceMO.setOwner(username);
        deviceMO.set(new Agent());
        deviceMO.set(new IsDevice());
        deviceMO.set(new Object(), "c8y_SNMPGateway");
        deviceMO = platformProvider.getTestPlatform().getInventoryApi().create(deviceMO);

        ExternalIDRepresentation externalId = new ExternalIDRepresentation();
        externalId.setType("c8y_Serial");
        externalId.setExternalId("snmp-agent");
        externalId.setManagedObject(deviceMO);
        platformProvider.getTestPlatform().getIdentityApi().create(externalId);
    }

    @And("^I stop gateway process$")
    public void stopGatewayProcess() throws InterruptedException {
        log.info("Shutting down gateway process...");
        gatewayProcess.stop();
    }

    @Then("Gateway device should be created")
    public void gatewayDeviceShouldExist() {
        checkGatewayDeviceAvailability();
    }

    @Then("There should exist only one gateway device in the platform")
    public void oneGatewayDeviceShouldExist() {
        checkGatewayDeviceAvailability();
    }

    @Then("Gateway process should stop")
    public void checkGatewayProcessStop() throws InterruptedException {
        long timeout = System.currentTimeMillis() + 25000;
        while (gatewayProcess.get().isAlive() && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }

        log.info(gatewayProcess.get().isAlive() + "");
        assertFalse(gatewayProcess.get().isAlive());
    }

    private boolean isGatewayDeviceExist() {
        log.info("Checking if gateway device exist...");
        Platform testPlatform = platformProvider.createIntegrationTestPlatform(tenantProvider.getTestTenantCredentials());

        return TaskExecutor.run(() -> {
            ID id = new ID("c8y_Serial", getSnmpGatewayDeviceName());
            try {
                ExternalIDRepresentation externalId = testPlatform.getIdentityApi().getExternalId(id);
                if (externalId != null) {
                    gatewayDevice = externalId.getManagedObject();
                    return true;
                }
                return false;
            } catch (SDKException e) {
                return false;
            }
        });
    }

    private String getConfigFilePath() {
        String propertyFileName = "snmp-agent-gateway" + tenantProvider.getTestTenant().getId() + ".properties";
        return Paths.get(System.getProperty("user.home"), ".snmp", propertyFileName).toString();
    }

    private String getDeviceCredentialsFilePath() {
        return Paths.get(System.getProperty("user.home"), ".snmp", "snmp-agent", "chronicle", "maps", "device-credentials-store.dat")
                .toString();
    }

    public void deleteGatewayConfiguration() throws IOException {
        log.info("Deleting gateway configuration");
        String configFilePath = getConfigFilePath();

        log.info("Deleting file: " + configFilePath);
        FileUtils.forceDelete(new File(configFilePath));
        assertFalse(new File(configFilePath).exists());
        log.info("Gateway configuration deleted");
    }

    public String getGatewayOwner() {
        return "device_" + getSnmpGatewayDeviceName();
    }

    private String getSnmpGatewayDeviceName() {
        return "snmp-agent-" + tenantProvider.getTestTenant().getId();
    }
}
