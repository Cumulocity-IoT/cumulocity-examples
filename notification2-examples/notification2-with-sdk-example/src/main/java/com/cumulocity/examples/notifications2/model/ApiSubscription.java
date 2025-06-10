package com.cumulocity.examples.notifications2.model;

import com.cumulocity.sdk.client.notification2.AckMode;
import com.cumulocity.sdk.client.notification2.DeviceContextTargetApi;
import com.cumulocity.sdk.client.notification2.Subscription;
import com.cumulocity.sdk.client.notification2.TenantContextTargetApi;
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
    private Boolean persistent;
    private AckMode ackMode;
    private TenantContextTargetApi tenantTopic;
    private String deviceId;
    private DeviceContextTargetApi deviceTopic;
    private String typeFilter;

    public Subscription toSubscription(String subscriber, String tenantId) {
        Subscription.Builder builder = Subscription.Builder.get();
        builder = builder.withId(name, subscriber).withTenantId(tenantId);
        if (shared != null) {
            builder = builder.withShared(shared);
        }
        if (persistent != null) {
            builder = builder.withPersistent(persistent);
        }
        if (ackMode != null) {
            builder = builder.withAckMode(ackMode);
        }
        if (tenantTopic != null) {
            builder = builder.withTenantContextTargetApis(tenantTopic);
        }
        else {
            builder = builder.withDeviceContextTargetApis(deviceId, deviceTopic);
        }
        if (typeFilter != null) {
            builder = builder.withTypeFilter(typeFilter);
        }
        return builder.build();
    }

    public Subscription.ID toSubscriptionId(String subscriber) {
        return new Subscription.ID(name, subscriber);
    }
}
