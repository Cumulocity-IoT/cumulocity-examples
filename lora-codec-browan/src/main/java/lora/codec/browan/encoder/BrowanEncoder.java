package lora.codec.browan.encoder;

import org.springframework.stereotype.Component;

import com.cumulocity.microservice.customencoders.api.exception.EncoderServiceException;
import com.cumulocity.microservice.customencoders.api.model.EncoderInputData;
import com.cumulocity.microservice.customencoders.api.model.EncoderResult;
import com.cumulocity.microservice.customencoders.api.service.EncoderService;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderResult;

@Component
public class BrowanEncoder implements EncoderService {

	@Override
	public EncoderResult encode(EncoderInputData arg0) throws EncoderServiceException {
        LpwanEncoderResult encoderResult = new LpwanEncoderResult();
        encoderResult.setSuccess(false);
        encoderResult.setMessage("Encoding Payload Failed");
		return encoderResult;
	}

}
