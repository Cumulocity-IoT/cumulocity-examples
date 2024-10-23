/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent.server;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import c8y.trackeragent.configuration.TrackerConfiguration;
import c8y.trackeragent.protocol.TrackingProtocol;
import c8y.trackeragent.server.TrackerServerEvent.CloseConnectionEvent;
import c8y.trackeragent.server.TrackerServerEvent.ReadDataEvent;
import c8y.trackeragent.server.writer.OutWriter;
import c8y.trackeragent.server.writer.OutWriterImpl;
import c8y.trackeragent.tracker.ConnectedTracker;
import c8y.trackeragent.tracker.ConnectedTrackerFactory;

@Component
public class TrackerServerEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(TrackerServerEventHandler.class);

    private final ExecutorService workers;
    private final ConnectedTrackerFactory connectedTrackerFactory;    
    private final ConnectionsContainer connectionsContainer;
    private final TrackerConfiguration trackerConfiguration;

    @Autowired
    public TrackerServerEventHandler(ConnectedTrackerFactory connectedTrackerFactory, ConnectionsContainer connectionsContainer, TrackerConfiguration trackerConfiguration) {
        this.connectedTrackerFactory = connectedTrackerFactory;
        this.connectionsContainer = connectionsContainer;
        this.trackerConfiguration = trackerConfiguration;
        this.workers = newFixedThreadPool(trackerConfiguration.getNumberOfReaderWorkers());
    }

    @PostConstruct
    public void init() {
        logger.info("Number of reader workers: {}.", trackerConfiguration.getNumberOfReaderWorkers());
    }
    
    public void shutdownWorkers(){
        this.workers.shutdown();
    }

    public void handle(ReadDataEvent readDataEvent) {
        try {
            ActiveConnection connection = getActiveConnection(readDataEvent);
            ReaderWorker worker = new ReaderWorker(
                    new IncomingMessage(
                            connection.getConnectionDetails(),
                            connection.getConnectedTracker(),
                            Arrays.copyOf(readDataEvent.getData(), readDataEvent.getNumRead()))
            );
            workers.execute(worker);
        } catch (Exception e) {
            logger.error("Exception handling read event " + readDataEvent, e);
        }
    }
    
    public void handle(CloseConnectionEvent closeConnectionEvent) {
        synchronized (connectionsContainer) {
            connectionsContainer.remove(closeConnectionEvent.getChannel());
        }
    }

    private ActiveConnection getActiveConnection(ReadDataEvent readEvent) throws Exception {
        synchronized (connectionsContainer) {
            ActiveConnection connection = connectionsContainer.get(readEvent.getChannel());
            if (connection == null) {
                connection = createConnection(readEvent);
                connectionsContainer.add(connection);
            }
            return connection;
        }
    }

    private ActiveConnection createConnection(ReadDataEvent readEvent) throws Exception {
        ConnectedTracker connectedTracker = connectedTrackerFactory.create(readEvent);
        TrackingProtocol trackingProtocol = connectedTracker.getTrackingProtocol();
        OutWriter outWriter = new OutWriterImpl(readEvent.getServer(), readEvent.getChannel());
        ConnectionDetails connectionDetails = new ConnectionDetails(trackingProtocol, outWriter,
                readEvent.getChannel(), connectionsContainer);
        return new ActiveConnection(connectionDetails, connectedTracker);
    }
}
