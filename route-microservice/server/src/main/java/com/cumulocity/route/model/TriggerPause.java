package com.cumulocity.route.model;

import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.route.model.core.HasMeasurement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TriggerPause extends HasMeasurement {
    private final MeasurementRepresentation measurement;

    public TriggerPause(SpeedMeasurement speedMeasurement) {
        this.measurement = speedMeasurement.getMeasurement();
    }
}
