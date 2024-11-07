/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent.utils;

import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import jakarta.annotation.Nullable;

import org.joda.time.DateTime;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.measurement.MeasurementValue;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.google.common.base.Function;
import com.google.common.base.Joiner;

import c8y.Position;
import c8y.SpeedMeasurement;
import c8y.trackeragent.device.TrackerDevice;

public class LocationEventBuilder {

    private Collection<AlarmRepresentation> alarms;
    private DateTime dateTime = new DateTime();
    private GId sourceId;
    private SpeedMeasurement speedMeasurement;
    private Position position = new Position();

    public static LocationEventBuilder aLocationEvent() {
        return new LocationEventBuilder();
    }

    public LocationEventBuilder withSourceId(GId sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public LocationEventBuilder withTime(Date time) {
        if (time != null) {
            this.dateTime = new DateTime(time);
        }
        return this;
    }
    
    public LocationEventBuilder withDateTime(DateTime dateTime) {
        if (dateTime != null) {
            this.dateTime = dateTime;
        }
        return this;
    }
    
    public LocationEventBuilder withSpeedMeasurement(SpeedMeasurement speedMeasurement) {
        this.speedMeasurement = speedMeasurement;
        return this;
    }

    public LocationEventBuilder withAlarm(AlarmRepresentation alarm) {
        if(alarm == null) {
            return this;
        } else {
            return withAlarms(asList(alarm));
        }
    }

    public LocationEventBuilder withAlarms(Collection<AlarmRepresentation> alarms) {
        this.alarms = alarms;
        return this;
    }
    
    public LocationEventBuilder withLat(BigDecimal lat) {
        position.setLat(lat);
        return this;
    }
    
    public LocationEventBuilder withLng(BigDecimal lng) {
        position.setLng(lng);
        return this;
    }
    
    public LocationEventBuilder withAlt(BigDecimal alt) {
        position.setAlt(alt);
        return this;
    }
    
    public LocationEventBuilder withPosition(Position position) {
        this.position = position;
        return this;
    }

    public EventRepresentation build() {
        EventRepresentation result = new EventRepresentation();
        result.setType(TrackerDevice.LU_EVENT_TYPE);
        result.setText(aText());
        result.setSource(asSource(sourceId));
        result.setDateTime(dateTime);
        result.set(position);
        if (speedMeasurement != null) {
            result.set(speedMeasurement);
        }
        return result;
    }
    
    private String aText() {
        List<String> parts = new ArrayList<String>();
        if (speedMeasurement != null) {
            parts.add(speedText(speedMeasurement));
        }
        if (alarms != null && !alarms.isEmpty()) {
            parts.add(alarmsText());
        }
        if (parts.isEmpty()) {
            parts.add("Location updated");
        }
        return Joiner.on(" ").join(parts);
    }

    private String alarmsText() {
        return Joiner.on("|").join(transform(alarms, ALARM_TO_TEXT));
    }

    private static String speedText(SpeedMeasurement speedMeasurement) {
        MeasurementValue speed = speedMeasurement.getSpeed();
        return Joiner.on(" ").skipNulls().join(speed.getValue(), speed.getUnit());
    }

    private static ManagedObjectRepresentation asSource(GId sourceId) {
        ManagedObjectRepresentation source = new ManagedObjectRepresentation();
        source.setId(sourceId);
        return source;
    }
    
    private static Function<AlarmRepresentation, String> ALARM_TO_TEXT = new Function<AlarmRepresentation, String>() {

        @Override
        public String apply(@Nullable AlarmRepresentation input) {
            return input.getText();
        }
    };
}
