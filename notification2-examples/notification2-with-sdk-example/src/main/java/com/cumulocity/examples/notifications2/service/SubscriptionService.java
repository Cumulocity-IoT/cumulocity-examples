package com.cumulocity.examples.notifications2.service;

import com.cumulocity.examples.notifications2.model.ApiSubscription;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.sdk.client.notification2.Notifications2Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SubscriptionService {
    @Autowired
    private Notifications2Api notifications2Api;

    @Autowired
    private MyNotificationListener listener;

    @Autowired
    private ContextService<Credentials> contextService;

    @Value("${subscriberId}")
    private String subscriberId;

    public void subscribe(ApiSubscription cmd) {
        notifications2Api.subscribe(cmd.toSubscription(subscriberId, contextService.getContext().getTenant()), listener);
    }

    public void disconnect(ApiSubscription cmd) {
        notifications2Api.disconnect(cmd.toSubscriptionId(subscriberId), true);
    }

    public void delete(ApiSubscription cmd) {
        notifications2Api.delete(cmd.toSubscriptionId(subscriberId));
    }
}
