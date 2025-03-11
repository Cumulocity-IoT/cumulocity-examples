package c8y.example.notification.microservice;

import c8y.example.notification.client.platform.SubscriptionRepository;
import c8y.example.notification.client.platform.TokenService;
import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import com.cumulocity.sdk.client.messaging.notifications.NotificationSubscriptionApi;
import com.cumulocity.sdk.client.messaging.notifications.TokenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

@MicroserviceApplication
public class HelloWorldConfiguration {

    @Bean
    public TokenService tokenService(TokenApi tokenApi) {
        return new TokenService(tokenApi);
    }

    @Bean
    public SubscriptionRepository subscriptionRepository(NotificationSubscriptionApi notificationSubscriptionApi) {
        return new SubscriptionRepository(notificationSubscriptionApi);
    }
}
