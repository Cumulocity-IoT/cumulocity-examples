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

package lora.codec.lansitec.encoder;

import com.cumulocity.microservice.customencoders.api.exception.EncoderServiceException;
import com.cumulocity.microservice.customencoders.api.model.EncoderInputData;
import com.cumulocity.microservice.customencoders.api.model.EncoderResult;
import com.cumulocity.microservice.customencoders.api.service.EncoderService;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderInputData;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderResult;
import com.cumulocity.model.idtype.GId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Component
public class LansitecEncoder implements EncoderService {
    public static final String SET_CONFIG = "set config";
    public static final String DEVICE_REQUEST = "device request";
    public static final String REGISTER_REQUEST = "register request";
    public static final String POSITION_REQUEST = "position request";

    @Override
    public EncoderResult encode(EncoderInputData encoderInputData) throws EncoderServiceException {
        LpwanEncoderInputData lpwanEncoderInputData = new LpwanEncoderInputData(GId.asGId(encoderInputData.getSourceDeviceId()),
                encoderInputData.getCommandName(),
                encoderInputData.getCommandData(),
                encoderInputData.getArgs());

        LpwanEncoderResult encoderResult = null;
        if (lpwanEncoderInputData.getSourceDeviceInfo().getDeviceManufacturer().equalsIgnoreCase("Lansitec") && lpwanEncoderInputData.getSourceDeviceInfo().getDeviceModel().equals("Asset Tracker")) {
            ObjectMapper mapper = new ObjectMapper();
            String payload = null;
            try {
                if (lpwanEncoderInputData.getCommandName().equals(POSITION_REQUEST)) {
                    payload = "A1FF";
                } else if (lpwanEncoderInputData.getCommandName().equals(REGISTER_REQUEST)) {
                    payload = "A2FF";
                } else if (lpwanEncoderInputData.getCommandName().equals(DEVICE_REQUEST)) {
                    payload = "A3FF";
                } else if (lpwanEncoderInputData.getCommandName().equals(SET_CONFIG)) {
                    JsonNode params = mapper.readTree(lpwanEncoderInputData.getCommandData());
                    ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
                    byte breakpoint = params.get("breakpoint").asBoolean() ? (byte) 8 : 0;
                    byte selfadapt = params.get("selfadapt").asBoolean() ? (byte) 4 : 0;
                    byte oneoff = params.get("oneoff").asBoolean() ? (byte) 2 : 0;
                    byte alreport = params.get("alreport").asBoolean() ? (byte) 1 : 0;
                    buffer.put((byte) ((byte) 0x90 | (byte) breakpoint | (byte) selfadapt | (byte) oneoff | (byte) alreport));
                    buffer.putShort((short) params.get("pos").asInt());
                    buffer.put((byte) params.get("hb").asInt());
                    payload = BaseEncoding.base16().encode(buffer.array());
                }

                encoderResult = new LpwanEncoderResult(payload, 20);
                encoderResult.setSuccess(true);
                encoderResult.setMessage("Successfully Encoded the payload");
            } catch (IOException e) {
                e.printStackTrace();
                encoderResult = new LpwanEncoderResult();
                encoderResult.setSuccess(false);
                encoderResult.setMessage("Encoding Payload Failed");
            }
        }
        return encoderResult;
    }
}
