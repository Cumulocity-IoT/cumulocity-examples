package lora.codec.browan;

import java.util.Collections;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import com.cumulocity.microservice.lpwan.codec.Codec;
import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;

@Component
public class BrowanCodec implements Codec {

	@Override
	public @NotNull @NotEmpty Set<DeviceInfo> supportsDevices() {
		
		DeviceInfo airQualitySensor = new DeviceInfo("Browan", "TBHV110");
		return Collections.singleton(airQualitySensor);
	}


}
