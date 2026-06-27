package c8y.example;

import com.cumulocity.microservice.settings.service.MicroserviceSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MicroserviceConfigurationTest {

    @Autowired
    ApplicationContext applicationContext;

    @MockitoBean
    MicroserviceSettingsService microserviceSettingsService;

    @Test
    void shouldLoadMicroserviceContext() {
        assertThat(applicationContext).isNotNull();
    }

}
