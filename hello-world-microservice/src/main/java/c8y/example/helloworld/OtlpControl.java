package c8y.example.helloworld;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.RuntimeMetrics;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

//@Component
@RestController
public class OtlpControl {

    @Autowired // commented out for disabling sdk
    C8yOpenTelemetrySdk c8yOtlpSdk;

    @Autowired // commented out for disabling sdk
    OpenTelemetrySdk otlpSdk;

    @Autowired // commented out for disabling sdk
    SdkMeterProvider sdkMeterProvider;

    @Autowired
    OtlpProperties otlpProperties;

    @Autowired
    CustomCounter customCounter;


    public void initOtlp() {
        c8yOtlpSdk.init();
    }

    @PostConstruct // commented out for disabling sdk
    private void initializeJvmMetrics() {
        RuntimeMetrics runtimeMetrics = RuntimeMetrics.builder(otlpSdk).build();
        String xxx = runtimeMetrics.toString();
    }

    @PostConstruct // commented out for disabling sdk
    private void runCustomCounter() {
        customCounter.createCounter(sdkMeterProvider);
    }


    @RequestMapping("updateTenantOptions")
    public String updateTenantOptions() {

        //OtlpProperties otlpProps = new OtlpProperties();
        otlpProperties.tenantOptionsUpdate();

        //OptionRepresentation option = tenantOptionsSource.getSecret("GRAFANA");
        return "done";
    }



}
