package c8y.example.helloworld;

import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GaugeStorage {
    private List<ObservableDoubleGauge> observableDoubleGaugeList = new ArrayList<>();

    public List<ObservableDoubleGauge> getObservableDoubleGaugeList() {
        return observableDoubleGaugeList;
    }

    public void addObservableGauge(ObservableDoubleGauge doubleGauge) {
        observableDoubleGaugeList.add(doubleGauge);
    }
}
