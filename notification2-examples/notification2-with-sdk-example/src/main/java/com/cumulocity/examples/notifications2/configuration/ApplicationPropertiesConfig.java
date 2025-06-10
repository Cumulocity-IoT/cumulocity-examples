package com.cumulocity.examples.notifications2.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources(value = {
        @PropertySource(value = "classpath:notifications2.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "file:/etc/notifications2/notifications2.properties", ignoreResourceNotFound = true)
})
public class ApplicationPropertiesConfig {
}