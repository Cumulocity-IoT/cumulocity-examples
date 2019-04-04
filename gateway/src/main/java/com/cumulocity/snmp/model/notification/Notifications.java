package com.cumulocity.snmp.model.notification;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.cep.notification.InventoryRealtimeDeleteAwareNotificationsSubscriber;
import com.cumulocity.sdk.client.cep.notification.ManagedObjectDeleteAwareNotification;
import com.cumulocity.sdk.client.devicecontrol.notification.OperationNotificationSubscriber;
import com.cumulocity.sdk.client.notification.Subscription;
import com.cumulocity.sdk.client.notification.SubscriptionListener;
import com.cumulocity.snmp.model.notification.platform.ManagedObjectListener;
import com.cumulocity.snmp.model.notification.platform.OperationListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Slf4j
@Service
public class Notifications {

    public InventoryRealtimeDeleteAwareNotificationsSubscriber subscribeInventory(PlatformParameters platform, GId id, final ManagedObjectListener listener) {
        final InventoryRealtimeDeleteAwareNotificationsSubscriber result = createInventorySubscriber(platform);
        result.subscribe(id.getValue(), new SubscriptionListener<String, ManagedObjectDeleteAwareNotification>() {
            @Override
            public void onNotification(Subscription<String> subscription, ManagedObjectDeleteAwareNotification notification) {
                if ("UPDATE".equals(notification.getRealtimeAction())) {
                    try {
                        listener.onUpdate(notification.getData());
                    } catch (InvocationTargetException e) {
                        log.error(e.getMessage(),e);
                    } catch (IllegalAccessException e) {
                        log.error(e.getMessage(),e);
                    }
                } else if ("DELETE".equals(notification.getRealtimeAction())) {
                    listener.onDelete();
                }
            }

            @Override
            public void onError(Subscription<String> subscription, Throwable throwable) {
                listener.onError(throwable);
            }
        });
        return result;
    }

    private InventoryRealtimeDeleteAwareNotificationsSubscriber createInventorySubscriber(PlatformParameters platform) {
        return new InventoryRealtimeDeleteAwareNotificationsSubscriber(platform);
    }

    public OperationNotificationSubscriber subscribeOperations(PlatformParameters platform, GId id, final OperationListener listener) {
        final OperationNotificationSubscriber result = createOperationsSubscriber(platform);
        result.subscribe(id, new SubscriptionListener<GId, OperationRepresentation>() {
            @Override
            public void onNotification(Subscription<GId> subscription, OperationRepresentation operation) {
                listener.onCreate(operation);
            }

            @Override
            public void onError(Subscription<GId> subscription, Throwable throwable) {
                listener.onError(throwable);
            }
        });
        return result;
    }

    private OperationNotificationSubscriber createOperationsSubscriber(PlatformParameters platform) {
        return new OperationNotificationSubscriber(platform);
    }
}
