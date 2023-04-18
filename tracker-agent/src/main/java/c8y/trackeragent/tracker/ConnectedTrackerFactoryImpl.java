/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent.tracker;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import c8y.trackeragent.configuration.TrackerConfiguration;
import c8y.trackeragent.protocol.TrackingProtocol;
import c8y.trackeragent.server.TrackerServerEvent.ReadDataEvent;

@Component
public class ConnectedTrackerFactoryImpl implements ConnectedTrackerFactory {

    private static final Logger logger = LoggerFactory.getLogger(ConnectedTrackerFactoryImpl.class);

    private final TrackerConfiguration config;
    private ListableBeanFactory beanFactory;

    @Autowired
    public ConnectedTrackerFactoryImpl(TrackerConfiguration config, ListableBeanFactory beanFactory) {
        this.config = config;
        this.beanFactory = beanFactory;
    }

    @Override
    public ConnectedTracker create(ReadDataEvent readData) throws Exception {
        logger.debug("Will peek tracker for new connection...");
        int localPort = readData.getChannel().socket().getLocalPort();

        byte markingByte = readData.getData()[0];

        logger.info("Marking byte: " + markingByte);
        ConnectedTracker result = create(localPort, markingByte);
        if (result == null) {
            throw new RuntimeException("No matching tracker found for first byte " + markingByte + " on port " + localPort);
        } else {
            logger.debug("Tracker for new connection: {}", result.getClass().getSimpleName());
        }
        return result;
    }

    private ConnectedTracker create(int localPort, byte markingByte) throws Exception {
        if (localPort == config.getLocalPort1()) {
            return discoverTracker(config.getLocalPort1Protocols(), markingByte);
        } else if (localPort == config.getLocalPort2()) {
            return discoverTracker(config.getLocalPort2Protocols(), markingByte);
        } else {
            throw new RuntimeException("Only support local ports: " + config.getLocalPort1() + ", " + config.getLocalPort2());
        }
    }

    private ConnectedTracker discoverTracker(Collection<TrackingProtocol> available, byte markingByte) throws Exception {
        for (TrackingProtocol trackerProtocol : available) {
            if (trackerProtocol.accept(markingByte)) {
                return beanFactory.getBean(trackerProtocol.getTrackerClass());
            }
        }
        return null;
    }
}
