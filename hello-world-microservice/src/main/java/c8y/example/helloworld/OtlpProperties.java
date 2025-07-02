package c8y.example.helloworld;

import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("otlpProperties")
@EnableScheduling
@Configuration
public class OtlpProperties {

    @Autowired
    C8yOpenTelemetrySdk c8yOpenTelemetrySdk;

    //@Autowired
    OtlpControl otlpControl;

    @Autowired
    private TenantOptionsProvider tenantOptionsProvider;


    Map<String, String> otelProperties = new HashMap<>();


    //@Scheduled(cron = "1,15,30,45 * * * * *")
    public void tenantOptionsUpdate() {
        setDefaultProperties();
        if(updateProperties()) {
            //otlpControl.initOtlp();
            c8yOpenTelemetrySdk.setOtlpProperties(otelProperties);
            c8yOpenTelemetrySdk.init();
        }
    }


    private boolean updateProperties() {
        printOtelProperties("Before retrieving tenant options");

        boolean updated = true; // TODO: should be "false"

        Map<String, String> newProps = getTenantOptionsFromC8y();
        for (Map.Entry<String, String> p : newProps.entrySet()) {
            String oldValue = otelProperties.get(p.getKey());
            if(oldValue != null) {
                if(!oldValue.equals(p.getValue())) {
                    otelProperties.replace(p.getKey(), p.getValue());
                    updated = true;
                }
            }
            else {
                otelProperties.put(p.getKey(), p.getValue());
                updated = true;
            }
        }

        printOtelProperties("After retrieving tenant options");

        return updated;
    }

    private Map<String, String> getTenantOptionsFromC8y() {
        List<OptionRepresentation> tenantOptions = tenantOptionsProvider.getTenantOptions();
        Map<String, String> props = new HashMap<>();
        tenantOptions.forEach(p -> props.put(p.getKey(), p.getValue()));
        return props;
    }

    Map<String, String> getTenantOptions() {
        return otelProperties;
    }


    public String getServiceName() {
        String serviceName = otelProperties.get("service.name");
        if(serviceName == null) {
            serviceName = "undefined";
        }
        return serviceName;
    }

    public Duration getTimeout() {
        String timeout = otelProperties.get("otel.exporter.otlp.timeout");
        if(timeout == null) {
            timeout = "10";
        }
        return Duration.ofSeconds(Long.parseLong(timeout));
    }

    public String getEndpoint() {
        return otelProperties.get("otel.exporter.otlp.endpoint");
    }

    public String getCompression() {
        return "someCompression";
    }


    private void printOtelProperties(String title) {
        System.out.println("\n-------------------- " + title + " ---------------------");
        for(Map.Entry<String, String> prop : otelProperties.entrySet().stream().toList()) {
            System.out.println(prop.getKey() + " = " + prop.getValue());
        }
        System.out.println("--------------------------------------------------------\n");
    }

    private void setDefaultProperties() {
        if(otelProperties.get("otel.exporter.otlp.timeout") == null) {
            otelProperties.put("otel.exporter.otlp.timeout", "10000");
        }
        if(otelProperties.get("service.name") == null) {
            otelProperties.put("service.name", "service-OTLP33");
        }
    }

}
