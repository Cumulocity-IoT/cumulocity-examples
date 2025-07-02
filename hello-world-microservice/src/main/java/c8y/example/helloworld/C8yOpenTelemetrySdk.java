package c8y.example.helloworld;

import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.logging.otlp.internal.metrics.OtlpStdoutMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;

@Component
@Configuration
@Service // commented out for disabling sdk
public class C8yOpenTelemetrySdk {

//    OTEL_EXPORTER_OTLP_ENDPOINT
//    OTEL_EXPORTER_OTLP_HEADERS
//    OTEL_EXPORTER_OTLP_TIMEOUT
//    OTEL_EXPORTER_OTLP_PROTOCOL

    //@Autowired
    //private OtlpProperties otelProperties;

    private SdkTracerProvider tracerProvider;
    private SdkMeterProvider meterProvider;
    private SdkLoggerProvider loggerProvider;
    private OpenTelemetrySdk otlpSdk;

    Map<String, String> otlpProperties = new HashMap<>();

    public void setOtlpProperties(Map<String, String> otlpProps) {
        this. otlpProperties = otlpProps;
    }

    private Map<String, String> getOtlpProperties() {
        return otlpProperties;
    }

    @PostConstruct // commented out for disabling sdk
    public void init() {
        setDefaultProperties();
        reconfigureOpenTelemetry();
    }

    @Bean // commented out for disabling sdk
    public OpenTelemetrySdk getOtlpSdk() {
        return otlpSdk;
    }

    @Lazy // commented out for disabling sdk
    @Bean // commented out for disabling sdk
    //@RefreshScope
    public SdkMeterProvider getSdkMeterProvider() {
        return otlpSdk.getSdkMeterProvider();
    }

    private void reconfigureOpenTelemetry() {
        //shutdownOldProviders();

        Resource resource = Resource.getDefault()
                .merge(Resource.builder().put(SERVICE_NAME, otlpProperties.get("service.name")).build())
                .merge(Resource.builder().put(SERVICE_VERSION, "gamma").build());

//        // Metrics
//        OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
//                .setEndpoint(otelProperties.getEndpoint())
//                .setHeaders(flattenHeaders(otelProperties.getHeaders()))
//                .setTimeout(otelProperties.getTimeout())
//                .setCompression(otelProperties.getCompression())
//                .build();

//        meterProvider = SdkMeterProvider.builder()
//                .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
//                        .setInterval(Duration.ofSeconds(30))
//                        .build())
//                .build();

        //Duration timeout = Duration.parse(otlpProperties.get(Integer.parseInt("otel.exporter.otlp.timeout")));
        Duration timeout = Duration.ofMillis(Integer.parseInt(otlpProperties.get("otel.exporter.otlp.timeout")));

        meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(
                        PeriodicMetricReader.builder(
                                        OtlpStdoutMetricExporter.builder().build())
                                //.setInterval(otelProperties.getTimeout())
                                .setInterval(timeout)
                                .build()
                )
                .build();

        // Combine into SDK
        otlpSdk = OpenTelemetrySdk.builder()
                //        .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                //        .setLoggerProvider(loggerProvider)
                //        .buildAndRegisterGlobal();
                .build();

        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(otlpSdk);

        printBeansList();
    }

    //RefresScope
    //https://dev-diaries.hashnode.dev/how-to-use-refreshscope-with-your-datasource-for-dynamic-property-updates-at-runtime
    // Use /actuator/refresh endpoint to dynamically fetch new property values without requiring an application restart

    // Vielleicht auch für Actuator refreshscope!!!???
    //The output from /metrics is produced by MetricsEndpoint.
    // It's available as a bean that you can have @Autowired.
    // Calling invoke on it should give you the data that you want.
    //You can do the same for /health with HealthEndpoint.

    // https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/application-context-services.html#refresh-scope
    // https://dev.to/saladlam/spring-cloud-refresh-scope-bean-2149
    // https://medium.com/@erayaraz10/introduction-to-refreshscope-annotation-95d5dab4bcf1
    // https://www.tutorialspoint.com/spring_boot/spring_boot_cloud_configuration_client.htm

    // possibly required: (from https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/application-context-services.html)
    //   bootstrap.yml
    //     spring:
    //       application:
    //         name: foo
    //     cloud:
    //       config:
    //         uri: ${SPRING_CONFIG_URI:http://localhost:8888}




    private void shutdownOldProviders() {
        if (tracerProvider != null) tracerProvider.close();
        if (meterProvider != null) meterProvider.close();
        if (loggerProvider != null) loggerProvider.close();
    }

    private String flattenHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return "";
        return headers.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

//    @Override
//    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
//        System.out.println("Detected OTLP config change. Reinitializing exporters...");
//        reconfigureOpenTelemetry();
//    }


//     public OpenTelemetrySdk createOtlpSdk() {
//
//         OpenTelemetrySdk openTelemetrySdk =
//                 OpenTelemetrySdk.builder()
// //                        .setTracerProvider(sdkTracerProviderStdout)
// //                        .setMeterProvider(sdkMeterProviderStdout)
//                         .setMeterProvider(getSdkMeterProviderStdout())
// //                    .setMeterProvider(sdkMeterProviderGrpc)
// //                    .setMeterProvider(sdkMeterProviderHttp)
// //                    .setLoggerProvider(sdkLoggerProviderStdout)
// //                    .setLoggerProvider(sdkLoggerProviderHttp)
//                         .buildAndRegisterGlobal();
//
//         return openTelemetrySdk;
//     }

    private SdkMeterProvider getSdkMeterProviderStdout() {

//        Resource resource = Resource.getDefault()
//                .merge(Resource.builder().put(SERVICE_NAME, "hello-sdk21").build());

        //getTenantOptions().forEach(Map.Entry<String, String> option -> {
        //    resource.merge(Resource.builder().put(option.getKey(), option.getValue()).build());
        //});

//        Map<String, String> tOptions = getTenantOptions();
//        for(Map.Entry<String, String> tOption : tOptions.entrySet()) {
//            resource.merge(Resource.builder().put(tOption.getKey(), tOption.getValue()).build());
//        }


        SdkMeterProvider sdkMeterProviderStdout = SdkMeterProvider.builder()
                //.setResource(resource)
                .registerMetricReader(
                        PeriodicMetricReader.builder(
                                        OtlpStdoutMetricExporter.builder().build())
                                .setInterval(Duration.ofMillis(10000))
                                .build()
                )
                .build();
        return sdkMeterProviderStdout;
    }


    private void setDefaultProperties() {
        if(otlpProperties.get("otel.exporter.otlp.timeout") == null) {
            otlpProperties.put("otel.exporter.otlp.timeout", "10000");
        }
        if(otlpProperties.get("service.name") == null) {
            otlpProperties.put("service.name", "service-OTLP33");
        }
    }

    private void printBeansList() {
        ApplicationContext context = new AnnotationConfigApplicationContext();
        String[] beanNames = context.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
            System.out.println(beanName);
        }
    }

//    private Map<String, String> getTenantOptions() {
//        Map<String, String> tenantOptions = otelProperties.getTenantOptions();
//        return tenantOptions;
//    }


}
