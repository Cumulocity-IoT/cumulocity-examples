package notifications2.service;

import com.cumulocity.sdk.client.notification2.api.NotificationListener;
import com.cumulocity.sdk.client.notification2.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This is the example implementation of the listener. All messages received in this service will end up here.
 * Implementing your own listener IS REQUIRED to use Notifications2Api
 */
@Component
@Slf4j
public class MyNotificationListener implements NotificationListener {

    @Override
    public void onMessage(Notification message, String subscriptionName) {
        log.info("Received message from subscription {}\n{}", subscriptionName, message);
    }
}
