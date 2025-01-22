/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent;

import c8y.trackeragent.exception.SDKExceptions;
import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.sdk.client.*;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.audit.AuditRecordApi;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApi;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;
import com.cumulocity.sdk.client.event.EventApi;
import com.cumulocity.sdk.client.event.EventBinaryApi;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.BinariesApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionApi;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import com.cumulocity.sdk.client.option.SystemOptionApi;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.cumulocity.sdk.client.user.UserApi;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.Callable;

public class TrackerPlatform implements Platform {

    private final Platform orig;
    private Cache<Class<?>, Object> cache;

    public TrackerPlatform(PlatformImpl orig) {
        this.orig = orig;
        this.cache = CacheBuilder.newBuilder().build();
    }

    public InventoryApi getInventoryApi() throws SDKException {
        return new CachedApiGetter<InventoryApi>(InventoryApi.class) {

            @Override
            public InventoryApi call() throws Exception {
                return orig.getInventoryApi();
            }

        }.get();
    }

    public IdentityApi getIdentityApi() throws SDKException {
        return new CachedApiGetter<IdentityApi>(IdentityApi.class) {

            @Override
            public IdentityApi call() throws Exception {
                return orig.getIdentityApi();
            }

        }.get();
    }

    public MeasurementApi getMeasurementApi() throws SDKException {
        return new CachedApiGetter<MeasurementApi>(MeasurementApi.class) {

            @Override
            public MeasurementApi call() throws Exception {
                return orig.getMeasurementApi();
            }

        }.get();
    }

    public DeviceControlApi getDeviceControlApi() throws SDKException {
        return new CachedApiGetter<DeviceControlApi>(DeviceControlApi.class) {

            @Override
            public DeviceControlApi call() throws Exception {
                return orig.getDeviceControlApi();
            }

        }.get();
    }

    public AlarmApi getAlarmApi() throws SDKException {
        return new CachedApiGetter<AlarmApi>(AlarmApi.class) {

            @Override
            public AlarmApi call() throws Exception {
                return orig.getAlarmApi();
            }

        }.get();
    }

    public EventApi getEventApi() throws SDKException {
        return new CachedApiGetter<EventApi>(EventApi.class) {

            @Override
            public EventApi call() throws Exception {
                return orig.getEventApi();
            }

        }.get();
    }

    public AuditRecordApi getAuditRecordApi() throws SDKException {
        return new CachedApiGetter<AuditRecordApi>(AuditRecordApi.class) {

            @Override
            public AuditRecordApi call() throws Exception {
                return orig.getAuditRecordApi();
            }

        }.get();
    }

    @Override
    public BinariesApi getBinariesApi() throws SDKException {
        return new CachedApiGetter<BinariesApi>(BinariesApi.class) {

            @Override
            public BinariesApi call() throws Exception {
                return orig.getBinariesApi();
            }

        }.get();
    }

    @Override
    public EventBinaryApi getEventBinaryApi() throws SDKException {
        return new CachedApiGetter<EventBinaryApi>(EventBinaryApi.class) {

            @Override
            public EventBinaryApi call() throws Exception{
                return orig.getEventBinaryApi();
            }
        }.get();
    }

    public DeviceCredentialsApi getDeviceCredentialsApi() throws SDKException {
        return new CachedApiGetter<DeviceCredentialsApi>(DeviceCredentialsApi.class) {

            @Override
            public DeviceCredentialsApi call() throws Exception {
                return orig.getDeviceCredentialsApi();
            }

        }.get();
    }

    public UserApi getUserApi() throws SDKException {
        return new CachedApiGetter<UserApi>(UserApi.class) {

            @Override
            public UserApi call() throws Exception {
                return orig.getUserApi();
            }

        }.get();
    }

    @Override
    public TenantOptionApi getTenantOptionApi() throws SDKException {
        return new CachedApiGetter<TenantOptionApi>(TenantOptionApi.class) {

            @Override
            public TenantOptionApi call() throws Exception {
                return orig.getTenantOptionApi();
            }

        }.get();
    }

    @Override
    public SystemOptionApi getSystemOptionApi() throws SDKException {
        return new CachedApiGetter<SystemOptionApi>(SystemOptionApi.class) {

            @Override
            public SystemOptionApi call() throws Exception {
                return orig.getSystemOptionApi();
            }
        }.get();
    }

    @Override
    public TokenApi getTokenApi() throws SDKException {
        return new CachedApiGetter<TokenApi>(TokenApi.class) {

            @Override
            public TokenApi call() throws Exception {
                return orig.getTokenApi();
            }
        }.get();
    }

    @Override
    public NotificationSubscriptionApi getNotificationSubscriptionApi() throws SDKException {
        return new CachedApiGetter<NotificationSubscriptionApi>(NotificationSubscriptionApi.class) {

            @Override
            public NotificationSubscriptionApi call() throws Exception {
                return orig.getNotificationSubscriptionApi();
            }
        }.get();
    }

    public void close() {
        orig.close();
    }

    public RestOperations rest() {
        return orig.rest();
    }

    public String getTenantId() {
        return getPlatformParameters().getTenantId();
    }

    public String getHost() {
        return getPlatformParameters().getHost();
    }

    public String getUser() {
        return getPlatformParameters().getUser();
    }

    public String getPassword() {
        CumulocityBasicCredentials cumulocityBasicCredentials = (CumulocityBasicCredentials) getPlatformParameters().getCumulocityCredentials();
        return cumulocityBasicCredentials.getPassword();
    }

    public PlatformParameters getPlatformParameters() {
        return (PlatformParameters) orig;
    }

    @Override
    public String toString() {
        return String.format("TrackerPlatform [orig=%s, getTenantId()=%s, getHost()=%s, getUser()=%s]", orig, getTenantId(), getHost(), getUser());
    }

    abstract class CachedApiGetter<V> implements Callable<V> {

        private final Class<V> cacheKey;

        CachedApiGetter(Class<V> cacheKey) {
            this.cacheKey = cacheKey;
        }

        @SuppressWarnings("unchecked")
        V get() throws SDKException {
            try {
                return (V) cache.get(cacheKey, this);
            } catch (Exception e) {
                throw SDKExceptions.narrow(e, "Cant create api " + cacheKey.getSimpleName());
            }
        }
    }

}
