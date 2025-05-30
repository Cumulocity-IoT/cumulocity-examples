package com.cumulocity.examples.notifications2.model;

import com.cumulocity.sdk.client.notification2.AckMode;
import com.cumulocity.sdk.client.notification2.DeviceNotificationTopic;
import com.cumulocity.sdk.client.notification2.Subscription;
import com.cumulocity.sdk.client.notification2.TenantNotificationTopic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ApiSubscription implements Serializable {
    private String name;
    private Boolean shared;
    private AckMode ackMode;
    private TenantNotificationTopic tenantTopic;
    private String deviceId;
    private DeviceNotificationTopic deviceTopic;
    private String typeFilter;

    public Subscription toSubscription(String consumer, String tenantId) {
        Subscription.Builder builder = Subscription.Builder.get();
        builder = builder.withId(name, consumer).withTenantId(tenantId);
        if (shared != null) {
            builder = builder.withShared(shared);
        }
        if (ackMode != null) {
            builder = builder.withAckMode(ackMode);
        }
        if (tenantTopic != null) {
            builder = builder.withTenantTopic(tenantTopic);
        }
        else {
            builder = builder.withDeviceTopic(deviceTopic, deviceId);
        }
        if (typeFilter != null) {
            builder = builder.withTypeFilter(typeFilter);
        }
        return builder.build();
    }

    public Subscription.ID toSubscriptionId(String consumer) {
        return new Subscription.ID(name, consumer);
    }
}
