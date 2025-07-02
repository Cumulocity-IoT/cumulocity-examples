package c8y.example.helloworld;

import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

//@Component
@RestController
public class CustomCounter {

//    @Autowired
//    C8yOpenTelemetrySdk c8yOpenTelemetrySdk;

    @Autowired // commented out for disabling sdk
    SdkMeterProvider sdkMeterProvider;

    //@Autowired
    OtlpProperties otlpProperties;

    //SdkMeterProvider meterProvider;
    LongCounter counter;


//    private OpenTelemetrySdk getOtlpSdk() {
//        //C8yOpenTelemetrySdk c8yOpenTelemetrySdk = new C8yOpenTelemetrySdk();
//        c8yOpenTelemetrySdk.init();
//        return c8yOpenTelemetrySdk.getOtlpSdk();
//    }
//
//    private SdkMeterProvider getSdkMeterProvider() {
//        return c8yOpenTelemetrySdk.getSdkMeterProvider();
//    }


    public void createCounter(SdkMeterProvider meterProvider) {
        //SdkMeterProvider meterProvider = meterProvider();
        //this.sdkMeterProvider = sdkMeterProvider;
        //SdkMeterProvider meterProvider = sdkMeterProvider;
        counter = sdkMeterProvider.meterBuilder("meter prototype").build().counterBuilder("simple-counter").build();
    }


    @RequestMapping("syncCounter")
    public String syncCounter() {

        //OtlpProperties otlpProps = new OtlpProperties();
        //otlpProperties.tenantOptionsUpdate();

        //OptionRepresentation option = tenantOptionsSource.getSecret("GRAFANA");

        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Attributes attrs = Attributes.of(stringKey("action.type"), "Check balance");
            counter.add(1, attrs);
        }
        return "Counter incremented";
    }




}
