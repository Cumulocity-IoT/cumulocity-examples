package c8y.example.mqttservice;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@MicroserviceApplication
@Slf4j
public class IntegrationMicroservice {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationMicroservice.class, args);
    }

    @Bean
    public RetryTemplate subscriptionRetryTemplate() {
        return RetryTemplate.builder()
                .infiniteRetry()
                .retryOn(Throwable.class)
                .fixedBackoff(5000)
                .build();
    }
    /** For Java 21+ use this **/
    /**
    @Bean("virtualThreadPool")
    public ExecutorService virtualThreadPool() {
        final ThreadFactory factory = Thread.ofVirtual().name("virtThread-", 0).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }
     **/

    /** For Java 17 or if you want to use a standard thread pool instead of virtual threads, use this **/
    @Bean("threadPool")  // Renamed for clarity; keep as "virtualThreadPool" if preferred
    public ExecutorService threadPool() {
        return Executors.newCachedThreadPool();
    }

}
