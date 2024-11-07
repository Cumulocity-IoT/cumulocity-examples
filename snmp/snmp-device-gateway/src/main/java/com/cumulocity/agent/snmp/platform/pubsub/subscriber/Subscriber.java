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

package com.cumulocity.agent.snmp.platform.pubsub.subscriber;

import com.cumulocity.agent.snmp.bootstrap.model.BootstrapReadyEvent;
import com.cumulocity.agent.snmp.platform.model.GatewayDataRefreshedEvent;
import com.cumulocity.agent.snmp.platform.pubsub.service.PubSub;
import com.cumulocity.agent.snmp.platform.service.GatewayDataProvider;
import com.cumulocity.agent.snmp.platform.service.PlatformProvider;
import com.cumulocity.sdk.client.SDKException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PreDestroy;
import java.util.Collection;

/**
 * Subscriber to handle messages,
 * which subscribes itself on BootstrapReadyEvent,
 * refreshes subscription on GatewayDataRefreshedEvent and
 * unsubscribes on PreDestroy Spring callback.
 *
 * @param <PS> PubSub service to subscribe to.
 */
@Slf4j
public abstract class Subscriber<PS extends PubSub<?>> {

    @Autowired
    private GatewayDataProvider gatewayDataProvider;

    @Autowired
    private PlatformProvider platformProvider;

    @Autowired
    private PS pubSub;


    private long transmitRateInSeconds = -1;


    public long getTransmitRateInSeconds() {
        return transmitRateInSeconds;
    }

    public boolean isReadyToAcceptMessages() {
        return platformProvider.isPlatformAvailable();
    }

    public abstract boolean isBatchingSupported();

    public abstract int getBatchSize();

    public abstract int getConcurrentSubscriptionsCount();

    public void onMessage(String message) throws SubscriberException {
        try {
            handleMessage(message);
        } catch(SDKException sdke) {
            if(sdke.getHttpStatus() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                // Unable to publish as the platform is unavailable
                platformProvider.markPlatfromAsUnavailable();
                log.debug("'{}' Subscriber has marked the platform as unavailable.", this.getClass().getSimpleName());
            }

            throw new SubscriberException(sdke.getMessage(), sdke);
        }
    }

    public void onMessages(Collection<String> messageCollection) throws SubscriberException {
        try {
            handleMessages(messageCollection);
        } catch(SDKException sdke) {
            if(sdke.getHttpStatus() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                // Unable to publish as the platform is unavailable
                platformProvider.markPlatfromAsUnavailable();
                log.debug("'{}' Subscriber has marked the platform as unavailable.", this.getClass().getSimpleName());
            }

            throw new SubscriberException(sdke.getMessage(), sdke);
        }
    }

    protected abstract void handleMessage(String message);

    protected void handleMessages(Collection<String> messageCollection) {
        throw new UnsupportedOperationException();
    }

    @EventListener(BootstrapReadyEvent.class)
    void subscribe() {
        this.transmitRateInSeconds = fetchTransmitRateFromGatewayDevice();

        pubSub.subscribe(this); // Subscribing for the first time

        log.debug("{} subscribed to {}.", this.getClass().getName(), pubSub.getClass().getName());
    }

    @EventListener(GatewayDataRefreshedEvent.class)
    void refreshSubscription() {
        if(!isBatchingSupported()) {
            return;
        }

        long newTransmitRateFromGatewayDevice = fetchTransmitRateFromGatewayDevice();
        if(transmitRateInSeconds != newTransmitRateFromGatewayDevice) {
            // Refresh the subscription only when the Transmit Rate
            // has changed for the Subscribers supporting batching
            pubSub.unsubscribe(this);

            // Update the transmit rate before resubscribing
            this.transmitRateInSeconds = newTransmitRateFromGatewayDevice;

            pubSub.subscribe(this);

            log.debug("{} refreshed its subscription as the transmit rate changed.", this.getClass().getName());
        }
    }

    @PreDestroy
    void unsubscribe() {
        pubSub.unsubscribe(this);

        log.debug("{} unsubscribed to {}.", this.getClass().getName(), pubSub.getClass().getName());
    }

    private long fetchTransmitRateFromGatewayDevice() {
        return gatewayDataProvider.getGatewayDevice().getSnmpCommunicationProperties().getTransmitRate();
    }
}
