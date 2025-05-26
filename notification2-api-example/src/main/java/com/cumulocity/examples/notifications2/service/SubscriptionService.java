package com.cumulocity.examples.notifications2.service;

import com.cumulocity.examples.notifications2.model.ApiSubscription;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.sdk.client.notification2.Notifications2Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SubscriptionService {
    private static final String CONSUMER = "notifications2Tester";

    @Autowired
    private Notifications2Api notifications2Api;

    @Autowired
    private MyNotificationListener listener;

    @Autowired
    private ContextService<Credentials> contextService;


    public void subscribe(ApiSubscription cmd) {
        notifications2Api.subscribe(cmd.toSubscription(CONSUMER, contextService.getContext().getTenant()), listener);
    }

    public void unsubscribe(ApiSubscription cmd) {
        notifications2Api.unsubscribe(cmd.toSubscriptionId(CONSUMER));
    }

    public void delete(ApiSubscription cmd) {
        notifications2Api.delete(cmd.toSubscriptionId(CONSUMER));
    }
}
