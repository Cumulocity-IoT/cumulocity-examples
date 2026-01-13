package c8y.example.mqttservice;

import c8y.example.mqttservice.service.PulsarClientService;
import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;



@MicroserviceApplication
@Slf4j
public class IntegrationMicroservice {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationMicroservice.class, args);
    }
}
