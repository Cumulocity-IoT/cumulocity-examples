package c8y.example.bytesequencedecoder;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.joda.JodaModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    // Declared as JsonMapper so that Spring Boot's JacksonAutoConfiguration backs off and this mapper is used.
    @Bean
    public JsonMapper objectMapper() {
        // Jackson 3 mappers are immutable, and date/time features moved to DateTimeFeature.
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(new JodaModule())
                .build();
    }
}
