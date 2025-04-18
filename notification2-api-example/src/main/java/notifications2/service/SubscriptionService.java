package notifications2.service;


import com.cumulocity.sdk.client.notification2.api.Notifications2Api;
import notifications2.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {
    /**
     * Consumer name must be unique, so if your microservice is clustered, you need to make sure that every instance has a unique identifier here.
     */
    private static final String CONSUMER = "notifications2Tester";

    @Autowired
    private Notifications2Api notifications2Api;

    @Autowired
    private MyNotificationListener listener;

    public void subscribe(Subscription cmd) {
        if (cmd.getDeviceTopic() != null && cmd.getDeviceId() != null) {
            notifications2Api.subscribeToDeviceTopic(CONSUMER, cmd.getDeviceTopic(), cmd.getDeviceId(), listener);
        }
        if (cmd.getTenantTopic() != null) {
            notifications2Api.subscribeToTenantTopic(CONSUMER, cmd.getTenantTopic(), listener);
        }
    }

    public void unsubscribe(Subscription cmd) {
        if (cmd.getDeviceTopic() != null && cmd.getDeviceId() != null) {
            notifications2Api.unsubscribeFromDeviceTopic(CONSUMER, cmd.getDeviceTopic(), cmd.getDeviceId());
        }
        if (cmd.getTenantTopic() != null) {
            notifications2Api.unsubscribeFromTenantTopic(CONSUMER, cmd.getTenantTopic());
        }
    }
}
