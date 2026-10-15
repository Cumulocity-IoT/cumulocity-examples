/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2022 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cumulocity.lora.codec.twtg.neon.decoder;

import com.cumulocity.lora.codec.twtg.neon.javascript.engine.JavaScriptEngine;
import com.cumulocity.microservice.customdecoders.api.exception.DecoderServiceException;
import com.cumulocity.microservice.customdecoders.api.model.DecoderResult;
import com.cumulocity.microservice.customdecoders.api.model.MeasurementDto;
import com.cumulocity.microservice.customdecoders.api.model.MeasurementValueDto;
import com.cumulocity.microservice.customdecoders.api.service.DecoderService;
import com.cumulocity.microservice.lpwan.codec.decoder.model.LpwanDecoderInputData;
import com.cumulocity.model.event.CumulocitySeverities;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import tools.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class NeonDecoder implements DecoderService {
    private final Logger logger = LoggerFactory.getLogger(NeonDecoder.class);

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    @Autowired
    private JavaScriptEngine graalJavaScriptEngineProxy;

    @Override
    public DecoderResult decode(String inputData, GId deviceId, Map<String, String> args) throws DecoderServiceException {
        LpwanDecoderInputData decoderData = new LpwanDecoderInputData(inputData, deviceId, args);

        try {
            // Decode the device payload by invoking the DecodeForGraaljs Javascript function,
            // a wrapper which in turn invokes the Decode() function from '/js/codec/decoder_ts_prot-2_doc-v2.2.1_rev-0'.
            //
            // This wrapper function (from '/js/codec/wrapper_functions.js') is created to overcome the
            // limitations of Graaljs with its handling of multilevel json objects and certain other data types.

            // Passing the inputData without any conversions assuming that it is a Base16 encoded string.
            String decodedString = graalJavaScriptEngineProxy.invokeFunction("DecodeForGraaljs", String.class, decoderData.getFport(), inputData);

            return processDecodedUplinkData(jsonObjectMapper.readValue(decodedString, Map.class));

        } catch (Exception e) {
            logger.error("Decoding payload failed. Error: " + e.getMessage(), e);

            // Create an alarm on the device, so the decoder issue is shown as an alarm
            AlarmRepresentation alarm = new AlarmRepresentation();
            alarm.setSource(ManagedObjects.asManagedObject(deviceId));
            alarm.setType("DecoderError");
            alarm.setSeverity(CumulocitySeverities.CRITICAL.name());
            alarm.setText(e.getMessage());
            alarm.setDateTime(DateTime.now());

            DecoderResult decoderResult = new DecoderResult();
            decoderResult.addAlarm(alarm, true);

            throw new DecoderServiceException(e, "Decoding payload failed. Error: " + e.getMessage(), decoderResult);
        }
    }

    /**
     * TODO: Replace with the uplink message processing logic
     *
     */
    private DecoderResult processDecodedUplinkData(Map<String, Object> decodedMap) throws Exception {
        if (!decodedMap.containsKey("application_event")) {
            throw new Exception("Error decoding the payload: 'application_event' field not available in the decoded result.");
        }
        Map<String, Object> applicationEventMap = (Map<String, Object>) decodedMap.get("application_event");

        if (!applicationEventMap.containsKey("temperature")) {
            throw new Exception("Error decoding the payload: 'temperature' field not available in the decoded result.");
        }
        Map<String, Object> temperatureMap = (Map<String, Object>) applicationEventMap.get("temperature");

        String minTemp = temperatureMap.get("min").toString();
        String maxTemp = temperatureMap.get("max").toString();
        String avgTemp = temperatureMap.get("avg").toString();

        // Add a measurement for average temperature value
        MeasurementDto measurementToAdd = new MeasurementDto();

        measurementToAdd.setType("c8y_Temperature");
        measurementToAdd.setSeries("c8y_Temperature");

        List<MeasurementValueDto> measurementValueDtos = new ArrayList<>();

        MeasurementValueDto minTempDto = new MeasurementValueDto();
        minTempDto.setSeriesName("min");
        minTempDto.setValue(new BigDecimal(minTemp));
        minTempDto.setUnit("C");
        measurementValueDtos.add(minTempDto);

        MeasurementValueDto maxTempDto = new MeasurementValueDto();
        maxTempDto.setSeriesName("max");
        maxTempDto.setValue(new BigDecimal(maxTemp));
        maxTempDto.setUnit("C");
        measurementValueDtos.add(maxTempDto);

        MeasurementValueDto avgTempDto = new MeasurementValueDto();
        avgTempDto.setSeriesName("avg");
        avgTempDto.setValue(new BigDecimal(avgTemp));
        avgTempDto.setUnit("C");
        measurementValueDtos.add(avgTempDto);

        measurementToAdd.setValues(measurementValueDtos);
        measurementToAdd.setTime(DateTime.now());

        DecoderResult decoderResult = new DecoderResult();
        decoderResult.addMeasurement(measurementToAdd);

        return decoderResult;
    }
}
