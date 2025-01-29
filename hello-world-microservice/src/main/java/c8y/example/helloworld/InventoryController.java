package c8y.example.helloworld;

import com.cumulocity.exporters.common.OpenTelemetryExporterStrategy;
import com.cumulocity.exporters.common.OpenTelemetryExporterStrategyEnum;
import com.cumulocity.exporters.common.OpenTelemetryExporterStrategyFactory;
import com.cumulocity.exporters.platform.ExportCompletedEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

@RestController
@Slf4j
public class InventoryController implements ApplicationListener<ExportCompletedEvent> {
    private static String INSTRUMENTATION_SCOPE_NAME = HelloWorldMain.class.getName();

    @Autowired
    private OpenTelemetryExporterStrategyFactory exporterStrategyFactory;

//    @Autowired
//    private OpenTelemetry openTelemetry;

    @Autowired
    private GaugeStorage gaugeStorage;

    private Meter meter;

    @GetMapping("/gaugeMetric")
    public String gaugeMetric() {
        OpenTelemetryExporterStrategy openTelemetryExporterStrategy = exporterStrategyFactory.findStrategy(OpenTelemetryExporterStrategyEnum.PLATFORM);
        OpenTelemetry openTelemetry = openTelemetryExporterStrategy.getOpenTelemetry();
        meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE_NAME);
        Random random = new Random();
        int value = random.nextInt(20);
        DateTime recordedTime = DateTime.now();
        log.info("Sending gaugeMetric with value {}", value);
        ObservableDoubleGauge gauge = meter
                .gaugeBuilder("hwGaugeMetricStSt")
                .buildWithCallback(
                        getObservableDoubleMeasurementConsumer(value, recordedTime));
        gaugeStorage.addObservableGauge(gauge);
        return "test";
    }

    private Consumer<ObservableDoubleMeasurement> getObservableDoubleMeasurementConsumer(int value, DateTime recordedTime) {
        return result -> result.record(value, Attributes.of(AttributeKey.stringKey(recordedTime.toString()), recordedTime.toString()));
    }

    @GetMapping("/sumMetric")
    public String sumMetric() {
        OpenTelemetryExporterStrategy openTelemetryExporterStrategy = exporterStrategyFactory.findStrategy(OpenTelemetryExporterStrategyEnum.PLATFORM);
        OpenTelemetry openTelemetry = openTelemetryExporterStrategy.getOpenTelemetry();
        meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE_NAME);
        log.info("Sending sumMetric at {}", LocalDateTime.now());
        Random random = new Random();
        LongCounter sumCounter = meter.counterBuilder("hwSumMetricStSt")
                .setUnit("units")
                .build();
        sumCounter.add(10);
        return "test";
    }

    @Override
    public void onApplicationEvent(ExportCompletedEvent event) {
        int metricCollectionSize = event.getMetricCollectionSize();
        List<ObservableDoubleGauge> gaugeList = gaugeStorage.getObservableDoubleGaugeList();
        for(int metricIndex = 0; metricIndex < metricCollectionSize; metricIndex++) {
            gaugeList.get(metricIndex).close();
        }
        gaugeList.subList(0, metricCollectionSize).clear();
    }
}
