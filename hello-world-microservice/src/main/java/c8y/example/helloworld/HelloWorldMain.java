
package c8y.example.helloworld;

// derived from: https://community.aws/content/2duS3aMiJhv3H7d3i1RvZsbNKxK/instrumenting-java-apps-using-opentelemetry?lang=en#custom-metrics-with-the-opentelemetry-sdk

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.logging.otlp.internal.logs.OtlpStdoutLogRecordExporter;
import io.opentelemetry.exporter.logging.otlp.internal.metrics.OtlpStdoutMetricExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import io.opentelemetry.sdk.resources.Resource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
//import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

//import static io.opentelemetry.instrumentation.runtimemetrics.*;
//import static io.opentelemetry.instrumentation.runtimemetrics.java8.RuntimeMetrics;
import        io.opentelemetry.instrumentation.runtimemetrics.java8.RuntimeMetrics;

@SpringBootApplication
@Configuration
//@PropertySource("classpath:application.properties")
@MicroserviceApplication
//@RestController
//@EnableConfigServer  // only for RefreshScope!
public class HelloWorldMain {

    private static final Logger log =
            LoggerFactory.getLogger(HelloWorldMain.class);
//**
//**    @Autowired
//**    private ConfigurationTenantOptions tenantOptionsSource;
//**
//**    @Autowired
//**    TenantOptionsProvider tenantOptionsProvider;
//**
//**//    @Autowired
//**//    PropertyUpdaterService propertyUpdater;
//**
//**    @Autowired
//**    private ConfigurableEnvironment environment;
//**
//**    @Autowired
//**    OtlpProperties otlpProps;
//**
//**    @Autowired  // TODO: does not yet work
//**    C8yOpenTelemetrySdk c8yOpenTelemetrySdk;
//**

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(HelloWorldMain.class, args);
        String[] beans = context.getBeanDefinitionNames();
        System.out.println("=======================================================================================================");
        for( String bn : beans) {
            System.out.println(bn);  // commented out for disabling sdk
        }
    }


    //@Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            System.out.println("Let's inspect the beans provided by Spring Boot:");
            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }
        };
    }


    //    @Value("${otel.exporter.otlp.protocol}")
    private String otlpProtocol;

    //    @Value("${otel.exporter.otlp.endpoint}")
    // correct endpoint!
//    private String otlpEndPoint;
    private String otlpEndPoint = "https://otlp-gateway-prod-eu-west-2.grafana.net/otlp/v1/metrics";

    //    @Value("${otel.exporter.otlp.headers}")
    // correct header!
    // omit "Authorization" !
//    private String otlpHeader = "Authorization=Basic MTAxNTI0OTpnbGNfZXlKdklqb2lNVEl3TURJek55SXNJbTRpT2lKemRHRmpheTB4TURFMU1qUTVMVzkwYkhBdGQzSnBkR1V0YUdWc2JHOHRiM1JzY0NJc0ltc2lPaUl6TVZCMlIyaE1Vek13UkRZMk9VcE5TVFZCTW5Jd1dXa2lMQ0p0SWpwN0luSWlPaUp3Y205a0xXVjFMWGRsYzNRdE1pSjlmUT09";
    private String otlpHeader = "Basic MTAxNTI0OTpnbGNfZXlKdklqb2lNVEl3TURJek55SXNJbTRpT2lKemRHRmpheTB4TURFMU1qUTVMVzkwYkhBdGQzSnBkR1V0YUdWc2JHOHRiM1JzY0NJc0ltc2lPaUl6TVZCMlIyaE1Vek13UkRZMk9VcE5TVFZCTW5Jd1dXa2lMQ0p0SWpwN0luSWlPaUp3Y205a0xXVjFMWGRsYzNRdE1pSjlmUT09";

    //    @Value("${otel.service.name}")
    private String otlpServiceName = "hello-sdk20";

    private String INSTRUMENTATION_SCOPE = "io.opentelemetry.example.metrics";

//**    Resource resource =
//**            Resource.getDefault()
//**             //       .merge(Resource.builder().put(SERVICE_NAME, "hello-sdk1-builder").build())
//**                    .merge(Resource.builder().put(SERVICE_VERSION, "alpha").build());
//**             //       .merge(Resource.builder().put("otel.exporter.otlp.endpoint", otlpEndPoint + "xxxx").build()); //is ignored!

    //example:  Resource.builder().put("service.name", "OtlpExporterExampleService").build());

    //tracerProvider = SdkTracerProvider.builder()
    //        .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "bar")))
    //        .build()

//**    Resource resource2 = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "bar"));
//**
//**    Resource resource3 =  Resource.getDefault().toBuilder()
//**            .put(ServiceAttributes.SERVICE_NAME, "my-service")
//**            .build();
//**
//**    // init OTel logger provider with export to OTLP
//**    SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder().setResource(resource)
//**            .addLogRecordProcessor(BatchLogRecordProcessor.builder(OtlpGrpcLogRecordExporter.builder()
//**                    .setEndpoint(otlpEndPoint).addHeader("Authorization", otlpHeader).build()).build()).build();
//**
//**    SdkLoggerProvider sdkLoggerProviderHttp = SdkLoggerProvider.builder().setResource(resource)
//**            .addLogRecordProcessor(BatchLogRecordProcessor.builder(OtlpHttpLogRecordExporter.builder()
//**                    .setEndpoint(otlpEndPoint).addHeader("Authorization", otlpHeader).build()).build()).build();
//**
//**    SdkLoggerProvider sdkLoggerProviderStdout = SdkLoggerProvider.builder().setResource(resource)
//**            .addLogRecordProcessor(BatchLogRecordProcessor.builder(OtlpStdoutLogRecordExporter.builder().build()).build()).build();
//**
//**    // init OTel trace provider with export to OTLP
//**    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().setResource(resource)
//**            .setSampler(Sampler.alwaysOn()).addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder()
//**                    .setEndpoint(otlpEndPoint).addHeader("Authorization", otlpHeader).build()).build()).build();
//**
//**    // init OTel meter provider with export to OTLP
//**    SdkMeterProvider sdkMeterProviderGrpc = SdkMeterProvider.builder().setResource(resource)
//**            .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder()
//**                    .setEndpoint(otlpEndPoint).addHeader("Authorization", otlpHeader).build()).build()).build();
//**
//**    // correct meter provider!
//**    SdkMeterProvider sdkMeterProviderHttp = SdkMeterProvider.builder().setResource(resource)
//**            .registerMetricReader(PeriodicMetricReader.builder(OtlpHttpMetricExporter.builder()
//**                    //.setEndpoint(otlpEndPoint).addHeader("Authorization", otlpHeader).build()).build()).build();
//**                    .addHeader("Authorization", otlpHeader).build()).build()).build();

    //otel.exporter.otlp.endpoint
    //otel.exporter.otlp.protocol
    //otel.exporter.otlp.headers

    //otel.propagators=tracecontext,b3
    //otel.resource.attributes.deployment.environment=dev
    //otel.resource.attributes.service.name=cart
    //otel.resource.attributes.service.namespace=shop

//**    SdkMeterProvider sdkMeterProviderStdout = SdkMeterProvider.builder()
//**            .setResource(resource)
//**            .registerMetricReader(
//**                    PeriodicMetricReader.builder(
//**                                    OtlpStdoutMetricExporter.builder().build())
//**                            .setInterval(Duration.ofMillis(10000))
//**                            .build()
//**            )
//**            .build();
//**
//**    SdkTracerProvider sdkTracerProviderStdout = SdkTracerProvider.builder()
//**            .setResource(resource)
//**            .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
//**            .build();
//**
//      OpenTelemetrySdk openTelemetrySdk =
//             OpenTelemetrySdk.builder()
// //                    .setTracerProvider(sdkTracerProviderStdout)
//                     .setMeterProvider(sdkMeterProviderStdout)
// //                    .setMeterProvider(sdkMeterProviderGrpc)
// //                    .setMeterProvider(sdkMeterProviderHttp)
// //                    .setLoggerProvider(sdkLoggerProviderStdout)
// //                    .setLoggerProvider(sdkLoggerProviderHttp)
// //                    .buildAndRegisterGlobal();
//                     .build();

//    OpenTelemetrySdk openTelemetrySdk = AutoConfiguredOpenTelemetrySdk.builder().build().getOpenTelemetrySdk();


//**    OpenTelemetrySdk openTelemetrySdk = getOtlpSdk();


    //boolean isRunning = runCustomCounter();

//**    RuntimeMetrics runtimeMetrics = RuntimeMetrics.builder(openTelemetrySdk).build();
//**
//**    Meter meter = openTelemetrySdk.getMeter("io.opentelemetry.example.meter");
//**    LongCounter counter = meter.counterBuilder("example.counter").build();
//**//    LongHistogram histogram = meter.histogramBuilder("super.timer").ofLongs().setUnit("ms").build();
//**
//**    LongHistogram histogram = meter.histogramBuilder("proc.time.histogram")
//**            .ofLongs()
//**            .setDescription("Processing time")
//**            .setExplicitBucketBoundariesAdvice(Arrays.asList(500L, 1000L, 5000L, 10000L, 20000L))
//**            .setUnit("msec")
//**            .build();
//**

//**    private  OpenTelemetrySdk getOtlpSdk() {
//**        //C8yOpenTelemetrySdk c8yOpenTelemetrySdk = new C8yOpenTelemetrySdk();
//**        c8yOpenTelemetrySdk.init();
//**        return c8yOpenTelemetrySdk.getOtlpSdk();
//**    }
//**
//**    private boolean runCustomCounter() {
//**        CustomCounter customCounter = new CustomCounter();
//**        customCounter.createCounter();
//**
//**        try {
//**            Thread.sleep(3600000);
//**        } catch (InterruptedException e) {
//**            throw new RuntimeException(e);
//**        }
//**        return true;
//**    }

//**    public OpenTelemetrySdk createOtlpSdk() {
//**
//**        OpenTelemetrySdk openTelemetrySdk =
//**                OpenTelemetrySdk.builder()
//**//                        .setTracerProvider(sdkTracerProviderStdout)
//**//                        .setMeterProvider(sdkMeterProviderStdout)
//**                        .setMeterProvider(getSdkMeterProviderStdout())
//**//                    .setMeterProvider(sdkMeterProviderGrpc)
//**//                    .setMeterProvider(sdkMeterProviderHttp)
//**//                    .setLoggerProvider(sdkLoggerProviderStdout)
//**//                    .setLoggerProvider(sdkLoggerProviderHttp)
//**                        .buildAndRegisterGlobal();
//**
//**        return openTelemetrySdk;
//**    }
//**
//**    private SdkMeterProvider getSdkMeterProviderStdout() {
//**
//**        Resource resource = Resource.getDefault()
//**                .merge(Resource.builder().put(SERVICE_NAME, "hello-sdk21").build());
//**
//**        getTenantOptions().forEach(option -> {
//**            resource.merge(Resource.builder().put(option.getKey(), option.getValue()).build());
//**        });
//**
//**
//**        SdkMeterProvider sdkMeterProviderStdout = SdkMeterProvider.builder()
//**                .setResource(resource)
//**                .registerMetricReader(
//**                        PeriodicMetricReader.builder(
//**                                        OtlpStdoutMetricExporter.builder().build())
//**                                .setInterval(Duration.ofMillis(10000))
//**                                .build()
//**                )
//**                .build();
//**        return sdkMeterProviderStdout;
//**    }
//**
//**    private List<OptionRepresentation> getTenantOptions() {
//**        List<OptionRepresentation> tenantOptions = tenantOptionsSource.getTenantOptions();
//**        return tenantOptions;
//**    }
//**

//**    @RequestMapping("setEnv")
//**    public String setEnvironmentParam() {
//**
//**        //GlobalOpenTelemetry.resetForTest();
//**
//**        System.out.println("\n\n------- Before environment update --------");
//**        PropertySource<?> pSource = environment.getPropertySources().get("Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'");
//**        String i = (String) pSource.getProperty("otel.metric.export.interval");
//**
//**        environment.getSystemProperties().forEach((key, value) -> {
//**            System.out.println("key: " + key + " , value: " + value);
//**        });
//**
//**//        propertyUpdater.updateProperty("otel.metric.export.interval", "15000");
//**
//**        System.out.println("------- After environment update --------\n\n");
//**        environment.getSystemProperties().forEach((key, value) -> {
//**            System.out.println("key: " + key + " , value: " + value);
//**        });
//**
//**        return "";
//**    }
//**
//**
//**    @RequestMapping("asyncGauge")
//**    public String asyncGauge() {
//**
//**        //DoubleGauge dg = meter.gaugeBuilder("free_memory").build();
//**        //dg.set(123);
//**
//**        meter.gaugeBuilder("free_memory")
//**                .setDescription("Available memory in bytes")
//**                .setUnit("bytes")
//**                .buildWithCallback(measurement -> {
//**                    measurement.record(
//**                            Runtime.getRuntime().freeMemory(),
//**                            Attributes.of(stringKey("Node-Id"), "Core-1")
//**                    );
//**                });
//**
//**        return "Async gauge initialized";
//**
//**    }
//**
//**
//**//    @RequestMapping("syncCounter")
//**    public String syncCounter() {
//**
//**        //OtlpProperties otlpProps = new OtlpProperties();
//**        otlpProps.tenantOptionsUpdate();
//**
//**        OptionRepresentation option = tenantOptionsSource.getSecret("GRAFANA");
//**
//**        for (int i = 0; i < 3; i++) {
//**            try {
//**                Thread.sleep(1000);
//**            } catch (InterruptedException e) {
//**                throw new RuntimeException(e);
//**            }
//**
//**            Attributes attrs = Attributes.of(stringKey("action.type"), "Check balance");
//**            counter.add(1, attrs);
//**        }
//**        return "Counter incremented";
//**    }
//**
//**
//**    @RequestMapping("histogram")
//**    public String histogram() {
//**        for (int i = 0; i < 3; i++) {
//**            long startTime = System.currentTimeMillis();
//**            randomWait();
//**            long currentTime = System.currentTimeMillis();
//**            histogram.record(currentTime - startTime, Attributes.of(stringKey("processType"), "process-A"));
//**        }
//**        return "Histogram recorded";
//**    }
//**
//**
//**    @RequestMapping("hello")
//**    public String greeting(@RequestParam(value = "who", defaultValue = "world") String who) {
//**
//**        log.info("xxxxxxxxxxxxxxxxxxxxxxxx Hello World! xxxxxxxxxxxxxxxxxxxxxxxxx");
//**        log.info("xxxxxxxxxxxxxxxxxxxxxxxx otlpProtocol = " + otlpProtocol + " xxxxxxxxxxxxxxxxxxxxxxxxx");
//**
//**        Attributes attributes = Resource.getDefault().getAttributes();
//**        Attributes attributesUpdated = resource.getAttributes();
//**
//**
//**        Tracer tracer = openTelemetrySdk.getTracer("io.opentelemetry.example.tracerxxxxxxxxxx.prometheus");
//**//        Meter meter = openTelemetrySdk.getMeter("io.opentelemetry.example.meterxxxxxxxxxx.prometheus");
//**//        LongCounter counter = meter.counterBuilder("example.counter").build();
//**//        LongHistogram histogram = meter.histogramBuilder("super.timer").ofLongs().setUnit("ms").build();
//**
//**        for (int i = 0; i < 3; i++) {
//**            long startTime = System.currentTimeMillis();
//**            Span exampleSpan = tracer.spanBuilder("exampleSpan").startSpan();
//**            Context exampleContext = Context.current().with(exampleSpan);
//**            try (Scope scope = exampleContext.makeCurrent()) {
//**                Attributes attrs = Attributes.of(stringKey("action.type"), "createxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
//**                counter.add(1, attrs);
//**
//**                exampleSpan.setAttribute("good", true);
//**                exampleSpan.setAttribute("exampleNumber", i);
//**                try {
//**                    Thread.sleep(1000);
//**                } catch (InterruptedException e) {
//**                    throw new RuntimeException(e);
//**                }
//**            } finally {
//**                histogram.record(
//**                        System.currentTimeMillis() - startTime, Attributes.of(stringKey("processType"), "process-A"));
//**                exampleSpan.end();
//**            }
//**        }
//**
//**        metricsDynatrace(meter);
//**
//**        // Creating a custom span <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
//**        Span span = tracer.spanBuilder("mySpan").startSpan();
//**        span.setAttribute("spanKey", "spanValue-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
//**        span.addEvent("spanevent-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
//**        String waitMillis = getMessageAfterWait();
//**        try (Scope scope = span.makeCurrent()) {
//**            if (waitMillis != "-1") {
//**                log.info("Wait finished");
//**            }
//**        } finally {
//**            span.end();  // Creating a custom span <<<<<<<<<<<<<<<<<<<<<
//**        }
//**
//**        return "hello " + who + "!  Waited for " + getMessageAfterWait();
//**    }
//**
//**    @WithSpan
//**    private String getMessageAfterWait() {
//**        Random r = new Random();
//**        int waitmilliSecs = r.nextInt(3000);
//**        try {
//**            Thread.sleep(waitmilliSecs);
//**        } catch (InterruptedException e) {
//**            throw new RuntimeException(e);
//**        }
//**        return String.valueOf(waitmilliSecs);
//**    }
//**
//**    private void randomWait() {
//**        Random r = new Random();
//**        int waitmilliSecs = r.nextInt(30000);
//**        try {
//**            Thread.sleep(waitmilliSecs);
//**        } catch (InterruptedException e) {
//**            throw new RuntimeException(e);
//**        }
//**    }
//**
//**
//**
//**    private void metricsDynatrace(Meter meter) {
//**
//**        LongCounter counter = meter.counterBuilder("request_counter")
//**                .setDescription("The number of requests we received-cccccccccccccccccccccccccccc")
//**                .setUnit("cntaaaaaaaaaaaaaaaaaaaaaaaaa")
//**                .build();
//**
//**        Attributes attrs = Attributes.of(stringKey("action.type"), "create-mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm");
//**        counter.add(1, attrs);
//**
//**        meter.gaugeBuilder("free_memory-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
//**                .setDescription("Available memory in bytes xxxxxxxxxxxxxxxxxxxxxxxxxx")
//**                .setUnit("bytes")
//**                .buildWithCallback(measurement -> {
//**                    measurement.record(
//**                            Runtime.getRuntime().freeMemory(),
//**                            Attributes.of(stringKey("user_count-xxxxxxxxxxxxxxxxxxxxxx"), getMessageAfterWait())
//**                    );
//**                });
//**    }
//**
}

