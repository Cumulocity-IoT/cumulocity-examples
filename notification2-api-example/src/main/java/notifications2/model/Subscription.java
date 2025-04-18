package notifications2.model;

import com.cumulocity.sdk.client.notification2.model.DeviceNotificationTopic;
import com.cumulocity.sdk.client.notification2.model.TenantNotificationTopic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * This is a model class that is accepted by test REST endpoints - it's not required by any means to have this to
 * use Notifications2Api
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Subscription implements Serializable {
    private String deviceId;
    private TenantNotificationTopic tenantTopic;
    private DeviceNotificationTopic deviceTopic;
}
