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

package com.cumulocity.agent.snmp.platform.pubsub.service;

import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.agent.snmp.persistence.Message;
import com.cumulocity.agent.snmp.persistence.Queue;
import com.cumulocity.agent.snmp.platform.pubsub.service.subscription.BatchMessagesSubscription;
import com.cumulocity.agent.snmp.platform.pubsub.service.subscription.SingleMessageSubscription;
import com.cumulocity.agent.snmp.platform.pubsub.service.subscription.Subscription;
import com.cumulocity.agent.snmp.platform.pubsub.subscriber.Subscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * A PubSub service which is used by publisher to publish message to a queue
 * and by subscriber to subscribe to a queue.
 *
 * @param <Q> Queue to PubSub
 */

@Slf4j
public abstract class PubSub<Q extends Queue> {

    private final TaskScheduler taskScheduler;
    private final GatewayProperties gatewayProperties;
    private final Q queue;
    private ScheduledFuture<?>[] subscriptions;

    public PubSub(TaskScheduler taskScheduler, GatewayProperties gatewayProperties, Q queue) {
        this.taskScheduler = taskScheduler;
        this.gatewayProperties = gatewayProperties;
        this.queue = queue;
    }

    public void publish(String message) {
        if(message == null) {
            throw new NullPointerException("message");
        }

        queue.enqueue(new Message(message));
    }

    public <PS extends PubSub<?>> void subscribe(Subscriber<PS> subscriber) {
        if(subscriptions != null) {
            throw new IllegalStateException("Duplicate subscriptions.");
        }

        Subscription newSubscription = null;
        Duration delayDuration = null;
        if(subscriber.isBatchingSupported() && subscriber.getTransmitRateInSeconds() > 0) {
            newSubscription = new BatchMessagesSubscription(queue, subscriber, gatewayProperties.getGatewayPublishRetryLimit());
            delayDuration = Duration.ofSeconds(subscriber.getTransmitRateInSeconds());
        }
        else {
            newSubscription = new SingleMessageSubscription(queue, subscriber, gatewayProperties.getGatewayPublishRetryLimit());
            delayDuration = Duration.ofMillis(1);
        }

        int concurrentSubscriptionsCount = subscriber.getConcurrentSubscriptionsCount();
        if(concurrentSubscriptionsCount > 0) {
            subscriptions = new ScheduledFuture[concurrentSubscriptionsCount];
            for(int i = 0; i< concurrentSubscriptionsCount; i++) {
                subscriptions[i] = taskScheduler.scheduleWithFixedDelay(newSubscription, delayDuration);
            }
        }

        log.debug("{} subscribed to queue {}", subscriber.getClass().getName(), queue.getName());
    }

    public void unsubscribe(Subscriber<?> subscriber) {
        if(subscriptions == null) {
            return;
        }

        for(ScheduledFuture<?> oneConcurrentSubscription : subscriptions) {
            oneConcurrentSubscription.cancel(true);
        }

        subscriptions = null;

        log.debug("{} unsubscribed to queue {}", subscriber.getClass().getName(), queue.getName());
    }
}
