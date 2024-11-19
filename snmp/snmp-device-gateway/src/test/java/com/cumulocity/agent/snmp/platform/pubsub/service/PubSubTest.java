/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. 
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided 
 * for in your License Agreement with Software AG.
 */

package com.cumulocity.agent.snmp.platform.pubsub.service;

import com.cumulocity.agent.snmp.config.GatewayProperties;
import com.cumulocity.agent.snmp.persistence.Message;
import com.cumulocity.agent.snmp.persistence.Queue;
import com.cumulocity.agent.snmp.platform.pubsub.service.subscription.Subscription;
import com.cumulocity.agent.snmp.platform.pubsub.subscriber.Subscriber;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class PubSubTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private GatewayProperties gatewayProperties;

    @Mock
    private Queue queue;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    @Mock
    private Subscriber<?> subscriber;

    @InjectMocks
    private PubSubImplForTest pubSub;

    @Captor
    private ArgumentCaptor<Subscription> subscriptionArgumentCaptor;

    @Test
    public void shouldPublishNonNullMessage() {
        String message = "MESSAGE STRING";
        pubSub.publish(message);

        Mockito.verify(queue).enqueue(messageCaptor.capture());
        assertEquals(message, messageCaptor.getValue().getPayload());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThowNPE_whenNullMessageIsPublished() {
        pubSub.publish(null);
    }

    @Test
    public void shouldSubscribeWhenBatchingIsNotSupported() {
        Mockito.when(subscriber.isBatchingSupported()).thenReturn(Boolean.FALSE);
        Mockito.when(gatewayProperties.getGatewayPublishRetryLimit()).thenReturn((short)2);

        int concurrentSubscriptionsCount = 3;
        Mockito.when(subscriber.getConcurrentSubscriptionsCount()).thenReturn(Integer.valueOf(concurrentSubscriptionsCount));

        ScheduledFuture<?> mockScheduledFuture = Mockito.mock(ScheduledFuture.class);
        Mockito.doReturn(mockScheduledFuture).when(taskScheduler).scheduleWithFixedDelay(Mockito.any(Subscription.class), Mockito.any(Duration.class));

        pubSub.subscribe(subscriber);

        Mockito.verify(taskScheduler, Mockito.times(concurrentSubscriptionsCount)).scheduleWithFixedDelay(subscriptionArgumentCaptor.capture(), Mockito.eq(Duration.ofMillis(1)));
        assertEquals(subscriber, subscriptionArgumentCaptor.getValue().getSubscriber());

        assertEquals(concurrentSubscriptionsCount, ((ScheduledFuture[])ReflectionTestUtils.getField(pubSub, "subscriptions")).length);
    }

    @Test
    public void shouldSubscribeWhenBatchingIsNotSupported_withZeroConcurrentSubscriptionsCount() {
        Mockito.when(subscriber.isBatchingSupported()).thenReturn(Boolean.FALSE);

        int concurrentSubscriptionsCount = 0;
        Mockito.when(subscriber.getConcurrentSubscriptionsCount()).thenReturn(Integer.valueOf(concurrentSubscriptionsCount));

        pubSub.subscribe(subscriber);

        Mockito.verifyNoInteractions(taskScheduler);

        assertNull(((ScheduledFuture[])ReflectionTestUtils.getField(pubSub, "subscriptions")));
    }

    @Test
    public void shouldSubscribeWhenBatchingIsSupported() {
        Mockito.when(subscriber.isBatchingSupported()).thenReturn(Boolean.TRUE);
        Mockito.when(gatewayProperties.getGatewayPublishRetryLimit()).thenReturn((short)2);

        long transmitRateInSeconds = 10;
        Mockito.when(subscriber.getTransmitRateInSeconds()).thenReturn(Long.valueOf(transmitRateInSeconds));

        int concurrentSubscriptionsCount = 3;
        Mockito.when(subscriber.getConcurrentSubscriptionsCount()).thenReturn(Integer.valueOf(concurrentSubscriptionsCount));

        ScheduledFuture<?> mockScheduledFuture = Mockito.mock(ScheduledFuture.class);
        Mockito.doReturn(mockScheduledFuture).when(taskScheduler).scheduleWithFixedDelay(Mockito.any(Subscription.class), Mockito.any(Duration.class));

        pubSub.subscribe(subscriber);

        Mockito.verify(taskScheduler, Mockito.times(concurrentSubscriptionsCount)).scheduleWithFixedDelay(subscriptionArgumentCaptor.capture(), Mockito.eq(Duration.ofSeconds(transmitRateInSeconds)));
        assertEquals(subscriber, subscriptionArgumentCaptor.getValue().getSubscriber());

        assertEquals(concurrentSubscriptionsCount, ((ScheduledFuture[])ReflectionTestUtils.getField(pubSub, "subscriptions")).length);
    }

    @Test
    public void shouldSubscribeWhenBatchingIsSupported_withZeroTransmitRateInSeconds() {
        Mockito.when(subscriber.isBatchingSupported()).thenReturn(Boolean.TRUE);
        Mockito.when(subscriber.getTransmitRateInSeconds()).thenReturn(Long.valueOf(0));
        Mockito.when(gatewayProperties.getGatewayPublishRetryLimit()).thenReturn((short)2);

        int concurrentSubscriptionsCount = 3;
        Mockito.when(subscriber.getConcurrentSubscriptionsCount()).thenReturn(Integer.valueOf(concurrentSubscriptionsCount));

        ScheduledFuture<?> mockScheduledFuture = Mockito.mock(ScheduledFuture.class);
        Mockito.doReturn(mockScheduledFuture).when(taskScheduler).scheduleWithFixedDelay(Mockito.any(Subscription.class), Mockito.any(Duration.class));

        pubSub.subscribe(subscriber);

        Mockito.verify(taskScheduler, Mockito.times(concurrentSubscriptionsCount)).scheduleWithFixedDelay(subscriptionArgumentCaptor.capture(), Mockito.eq(Duration.ofMillis(1)));
        assertEquals(subscriber, subscriptionArgumentCaptor.getValue().getSubscriber());

        assertEquals(concurrentSubscriptionsCount, ((ScheduledFuture[])ReflectionTestUtils.getField(pubSub, "subscriptions")).length);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfAlreadySubscribed() {
        Mockito.when(subscriber.isBatchingSupported()).thenReturn(Boolean.FALSE);
        Mockito.when(gatewayProperties.getGatewayPublishRetryLimit()).thenReturn((short)2);

        int concurrentSubscriptionsCount = 2;
        Mockito.when(subscriber.getConcurrentSubscriptionsCount()).thenReturn(Integer.valueOf(concurrentSubscriptionsCount));

        pubSub.subscribe(subscriber);
        pubSub.subscribe(subscriber);
    }

    @Test
    public void shouldUnsubscribe() {
        ScheduledFuture<?> mockScheduledFuture = Mockito.mock(ScheduledFuture.class);
        ReflectionTestUtils.setField(pubSub, "subscriptions", new ScheduledFuture[] {mockScheduledFuture, mockScheduledFuture});

        pubSub.unsubscribe(subscriber);

        Mockito.verify(mockScheduledFuture, Mockito.times(2)).cancel(true);

        assertNull(((ScheduledFuture[])ReflectionTestUtils.getField(pubSub, "subscriptions")));
    }

    @Test
    public void shouldUnsubscribe_withNullConcurrentSubscriptions() {
        ScheduledFuture<?> mockScheduledFuture = Mockito.mock(ScheduledFuture.class);
        ReflectionTestUtils.setField(pubSub, "subscriptions", null);

        pubSub.unsubscribe(subscriber);

        Mockito.verifyNoInteractions(mockScheduledFuture);

        assertNull(((ScheduledFuture[])ReflectionTestUtils.getField(pubSub, "subscriptions")));
    }

    private static class PubSubImplForTest extends PubSub<Queue> {
        public PubSubImplForTest(TaskScheduler taskScheduler, GatewayProperties gatewayProperties, Queue queue) {
            super(taskScheduler, gatewayProperties, queue);
        }
    }
}