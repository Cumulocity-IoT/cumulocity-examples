package c8y.lora.codec;

import com.cumulocity.lpwan.codec.model.Decode;
import com.cumulocity.lpwan.codec.model.Encode;
import com.cumulocity.lpwan.codec.service.BaseDeviceCodecService;
import com.cumulocity.lpwan.devicetype.model.DecodedDataMapping;
import com.cumulocity.lpwan.devicetype.model.UplinkConfigurationMapping;
import com.cumulocity.lpwan.mapping.model.DecodedObject;
import com.cumulocity.lpwan.payload.uplink.model.AlarmMapping;
import com.cumulocity.lpwan.payload.uplink.model.EventMapping;
import com.cumulocity.lpwan.payload.uplink.model.MeasurementMapping;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class GenericCodecService implements BaseDeviceCodecService {

    @Override
    public DecodedDataMapping decode(Decode decode) {
        log.info("Decoding logic goes here");
        UplinkConfigurationMapping uplinkConfigurationMapping = new UplinkConfigurationMapping();
        createMappings(uplinkConfigurationMapping);
        return new DecodedDataMapping(getDecodedObject(), uplinkConfigurationMapping);
    }

    private DecodedObject getDecodedObject() {
        DecodedObject decodedObject = new DecodedObject();
        decodedObject.putValue(29.55);
        decodedObject.putUnit("C");
        return decodedObject;
    }

    private void createMappings(UplinkConfigurationMapping uplinkConfigurationMapping) {
        AlarmMapping alarmMapping = new AlarmMapping();
        alarmMapping.setSeverity("MAJOR");
        alarmMapping.setText("Codec Alarm");
        alarmMapping.setType("c8y_CodecAlarm");
        uplinkConfigurationMapping.setAlarmMapping(alarmMapping);

        EventMapping eventMapping = new EventMapping();
        eventMapping.setText("Codec Event");
        eventMapping.setType("Codec Event Type");
        eventMapping.setFragmentType("c8y_CodecEventType");
        uplinkConfigurationMapping.setEventMapping(eventMapping);

        MeasurementMapping measurementMapping = new MeasurementMapping();
        measurementMapping.setType("c8y_CodecMeasurementMapping");
        measurementMapping.setSeries("Codec_Measurement_Mapping");
        uplinkConfigurationMapping.setMeasurementMapping(measurementMapping);
    }

    @Override
    public String encode(Encode encode) {
        log.info("Encoding logic goes here");
        return "Encoded String";
    }

    @Override
    public List<String> getModels() {
        return Arrays.asList("V1","V2","V3");
    }

    @Override
    public JSONObject getMetData() {
        return new JSONObject("Metadata");
    }
}
