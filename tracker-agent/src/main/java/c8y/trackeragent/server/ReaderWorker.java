/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;

public class ReaderWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ReaderWorker.class);

    @NotNull
    private final IncomingMessage incomingMessage;

    public ReaderWorker(IncomingMessage incomingMessage) {
        this.incomingMessage = incomingMessage;
    }

    @Override
    public void run() {
        logger.debug("Processing {}.", incomingMessage);
        try {
            incomingMessage.process();
        } catch (Exception ex) {
            logger.error("Error processing {} !", incomingMessage, ex);
        }
    }

}