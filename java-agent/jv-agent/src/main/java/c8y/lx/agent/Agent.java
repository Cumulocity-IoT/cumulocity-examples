/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package c8y.lx.agent;

import static c8y.lx.agent.CredentialsManager.defaultCredentialsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c8y.Hardware;
import c8y.IsDevice;
import c8y.RequiredAvailability;
import c8y.lx.driver.Configurable;
import c8y.lx.driver.DeviceManagedObject;
import c8y.lx.driver.Driver;
import c8y.lx.driver.HardwareProvider;
import c8y.lx.driver.OperationExecutor;

import com.cumulocity.model.ID;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.PlatformImpl;
import com.cumulocity.sdk.client.SDKException;

import jakarta.ws.rs.ProcessingException;

/**
 * <p>
 * Main executable agent class. Discovers installed drivers, manages the
 * drivers' life cycles and reports errors during startup as alarm (if
 * possible). Credentials for connecting to the platform are expected in a
 * configuration file, by default <code>/etc/cumulocity.properties</code>.
 * </p>
 * <p>
 * Working directory should be the directory with the installed jars for
 * software management to work.
 * </p>
 *
 * @see {@link Driver}, {@link CredentialsManager}
 */
public class Agent {

    private static final Logger logger = LoggerFactory.getLogger(Agent.class);

    public static final String TYPE = "c8y_Linux";
    public static final String XTIDTYPE = "c8y_Serial";
    public static final String ALARMTYPE = "c8y_AgentStartupError";
    public static final long RETRY_WAIT_MS = 5000L;
    public static final int RESPONSE_INTERVAL_MIN = 3; // We expect the agent to
                                                       // get back at least
                                                       // every three minutes.

    private final Platform platform;
    private final List<Driver> drivers;
    private final ManagedObjectRepresentation mo = new ManagedObjectRepresentation();
    private final DeviceBootstrapProcessor deviceBootstrapProcessor;

    public static void main(String[] args) {
        try {
            new Agent();
        } catch (Exception x) {
            logger.error("Unrecoverable error, exiting", x);
        }
    }

    public Agent() {
        this(defaultCredentialsManager(), new ServiceLocatorDriversLoader());
    }

    public Agent(CredentialsManager credentialsManager, DriversLoader driversLoader) {
        logger.info("Starting agent");
        this.deviceBootstrapProcessor = new DeviceBootstrapProcessor(credentialsManager);
        // See {@link Driver} for an explanation of the driver life cycle.
        this.drivers = initializeDrivers(driversLoader);
        this.platform = initializePlatform(credentialsManager);

        initializeDriverPlatforms();
        Map<String, ArrayList<OperationExecutor>> dispatchMap = initializeInventory();
        discoverChildren();
        startDrivers();
        new OperationDispatcher(this.platform.getDeviceControlApi(), mo.getId(), dispatchMap);
    }

    private Platform initializePlatform(CredentialsManager credentialsManager) {
        CumulocityCredentials credentials = credentialsManager.getDeviceCredentials();
        if (credentials == null) {
            Hardware hardware = specifyHardware();
            credentials = deviceBootstrapProcessor.process(hardware.getSerialNumber());
        }
        if (credentials == null) {
            throw new RuntimeException("Can't bootstrap device!");
        }
        PlatformImpl platform = new PlatformImpl(credentialsManager.getHost(), credentials);
        platform.setForceInitialHost(true);
        return platform;
    }

    private Hardware specifyHardware() {
        for (Driver driver : drivers) {
            if (driver instanceof HardwareProvider) {
                logger.info("Hardware provider present " + driver.getClass());
                Hardware hardware = ((HardwareProvider) driver).getHardware();
                if(!HardwareProvider.UNKNOWN.equals(hardware.getSerialNumber())) {
                    return hardware;
                }
            }
        }
        throw new IllegalStateException("None of dirvers implements HardwareProvider interface or there is uninitailized hardware!");
    }

    private List<Driver> initializeDrivers(DriversLoader driversLoader) {
        List<Driver> drivers = new LinkedList<>();
        ConfigurationDriver cfgDriver = null;
        logger.info("Initializing drivers");

        for (Driver driver : driversLoader.loadDrivers()) {
            try {
                logger.info("Initializing " + driver.getClass());
                driver.initialize();
                drivers.add(driver);

                if (driver instanceof ConfigurationDriver) {
                    cfgDriver = (ConfigurationDriver) driver;
                }
            } catch (Exception e) {
                logger.warn("Skipping driver " + driver.getClass());
                logger.debug("Driver error message: ", e);
            } catch (UnsatisfiedLinkError error) {
                logger.warn("Skipping driver " + driver.getClass());
                logger.debug("Driver error message: " + driver.getClass(), error);
            }
        }

        /*
         * ConfigurationDriver notifies other drivers of changes in
         * configuration if they implement Configurable.
         */
        if (cfgDriver != null) {
            for (Driver driver : drivers) {
                if (driver instanceof Configurable) {
                    cfgDriver.addConfigurable((Configurable) driver);
                }
            }
        }

        return drivers;
    }

    private void initializeDriverPlatforms() {
        for (Driver driver : drivers) {
            try {
                driver.initialize(platform);
            } catch (Exception e) {
                logger.error("Can't initialize driver platform " + driver.getClass(), e);
            }
        }
    }

    private Map<String, ArrayList<OperationExecutor>> initializeInventory() throws SDKException {
        logger.info("Initializing inventory");

        Map<String, ArrayList<OperationExecutor>> dispatchMap = new HashMap<>();
        for (Driver driver : drivers) {
            driver.initializeInventory(mo);

            for (OperationExecutor exec : driver.getSupportedOperations()) {
                String supportedOp = exec.supportedOperationType();
                if(dispatchMap.containsKey(supportedOp)){
                	dispatchMap.get(supportedOp).add(exec);
                }
                else {
                	ArrayList<OperationExecutor> newList = new ArrayList<OperationExecutor>();
                	newList.add(exec);
                	dispatchMap.put(supportedOp, newList);
                }
            }
        }

        Hardware hardware = mo.get(Hardware.class);
        String model = hardware.getModel();
        String serial = hardware.getSerialNumber();

        ID extId = asExternalId(hardware);

        mo.setType(TYPE);
        mo.setName(model + " " + serial);
        mo.set(new com.cumulocity.model.Agent());
        mo.set(new IsDevice());
        mo.set(new RequiredAvailability(RESPONSE_INTERVAL_MIN));

        checkConnection();

        logger.debug("Agent representation is {}, updating inventory", mo);

        if (new DeviceManagedObject(platform).createOrUpdate(mo, extId, null)) {
            logger.debug("Agent was created in the inventory");
        } else {
            logger.debug("Agent was updated in the inventory");
        }

        return dispatchMap;
    }

    private static ID asExternalId(Hardware hardware) {
        String id = "linux-" + hardware.getSerialNumber();
        ID extId = new ID(id);
        extId.setType(XTIDTYPE);
        return extId;
    }

    private void checkConnection() throws SDKException {
        logger.info("Checking platform connectivity");
        boolean connected = false;

        while (!connected) {
            try {
                platform.getInventoryApi().getManagedObjects().get(1);
                connected = true;
            } catch (ProcessingException x) {
                logger.debug("No connectivity, wait and retry", x);
                try {
                    Thread.sleep(RETRY_WAIT_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void discoverChildren() {
        logger.info("Discovering child devices");
        for (Driver driver : drivers) {
            driver.discoverChildren(mo);
        }
    }

    private void startDrivers() {
        logger.info("Starting drivers");
        for (Driver driver : drivers) {
            driver.start();
        }
    }
}
