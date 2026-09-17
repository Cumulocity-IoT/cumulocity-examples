package lora.codec.browan.decoder;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

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
import com.google.common.io.BaseEncoding;

@Component
public class BrowanDecoder implements DecoderService {

	@Override
	public DecoderResult decode(String inputData, GId deviceId, Map<String, String> args) throws DecoderServiceException {
		LpwanDecoderInputData decoderInput = new LpwanDecoderInputData(inputData, deviceId, args);
		ByteBuffer buffer = ByteBuffer.wrap(BaseEncoding.base16().decode(inputData.toUpperCase()));
		DecoderResult result = new DecoderResult();
		
		if(decoderInput.getFport() == 103) {
			buffer.get();
			buffer.get();
			int pcbTemperature = Byte.toUnsignedInt(buffer.get()) - 32;
			result.addMeasurement(measurement(decoderInput, "pcb_Temperature", "T", pcbTemperature, "C"));
			int humidity = Byte.toUnsignedInt(buffer.get());
			result.addMeasurement(measurement(decoderInput, "c8y_Humidity", "H", humidity, "%"));
			int co2 = Short.toUnsignedInt(Short.reverseBytes(buffer.getShort()));
			result.addMeasurement(measurement(decoderInput, "CO2", "C", co2, "ppm"));
			int voc = Short.toUnsignedInt(Short.reverseBytes(buffer.getShort()));
			result.addMeasurement(measurement(decoderInput, "VOC", "V", voc, "ppm"));
			int iaq = Short.toUnsignedInt(Short.reverseBytes(buffer.getShort()));
			result.addMeasurement(measurement(decoderInput, "IAQ", "Q", iaq, "Q"));
			int envTemperature = Byte.toUnsignedInt(buffer.get()) - 32;
			result.addMeasurement(measurement(decoderInput, "c8y_Temperature", "T", envTemperature, "C"));	
			
			if(iaq > 100) {
				result.addAlarm(alarm(deviceId, "AirQualityWarning", CumulocitySeverities.WARNING, 
						String.format("Air quality indicator > 100. Actual value:  %d. Please open window", iaq)), true);	
			}
		} else {
			result.addAlarm(alarm(deviceId, "DecoderError", CumulocitySeverities.CRITICAL, String.format("Unknown port: %s", decoderInput.getFport())), true);
		}
		
		return result;
	}

	private AlarmRepresentation alarm(GId deviceId, String alarmType, CumulocitySeverities severity, String alarmText) {
		AlarmRepresentation alarm = new AlarmRepresentation();
		alarm.setSource(ManagedObjects.asManagedObject(deviceId));
		alarm.setType(alarmType);
		alarm.setSeverity(severity.name());
		alarm.setText(alarmText);
		alarm.setDateTime(DateTime.now());
		return alarm;
	}

	private MeasurementDto measurement(LpwanDecoderInputData decoderInput, String fragmentName, String seriesName, long v, String unit) {
		MeasurementDto m = new MeasurementDto();
		m.setType(fragmentName);
		m.setSeries(fragmentName);
		MeasurementValueDto value = new MeasurementValueDto();
		value.setSeriesName(seriesName);
		value.setValue(BigDecimal.valueOf(v));
		value.setUnit(unit);
		m.setValues(Collections.singletonList(value));
		m.setTime(new DateTime(decoderInput.getUpdateTime() != null?decoderInput.getUpdateTime() : System.currentTimeMillis()));
		return m;
	}
}
