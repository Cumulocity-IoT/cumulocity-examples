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
import com.cumulocity.microservice.customencoders.api.model.EncoderResult;
import com.cumulocity.microservice.lpwan.codec.encoder.model.LpwanEncoderInputData;
import com.cumulocity.model.idtype.GId;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static lora.codec.lansitec.encoder.LansitecEncoder.DEVICE_REQUEST;
import static lora.codec.lansitec.encoder.LansitecEncoder.POSITION_REQUEST;
import static lora.codec.lansitec.encoder.LansitecEncoder.REGISTER_REQUEST;
import static lora.codec.lansitec.encoder.LansitecEncoder.SET_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LansitecEncoderTest {

    private final LansitecEncoder encoder = new LansitecEncoder();

    @Test
    void shouldEncodeSetConfigWithAllParameters() throws EncoderServiceException {
        // 0x90 | 8 | 4 | 2 | 1 = 0x9F, pos = 300 (0x012C), hb = 10 (0x0A)
        String command = "{\"breakpoint\":true,\"selfadapt\":true,\"oneoff\":true,\"alreport\":true,\"pos\":300,\"hb\":10}";

        EncoderResult result = encoder.encode(input(SET_CONFIG, command));

        assertTrue(result.isSuccess());
        assertEquals("9F012C0A", result.getEncodedCommand());
    }

    @Test
    void shouldEncodeSetConfigWithFalseFlags() throws EncoderServiceException {
        String command = "{\"breakpoint\":false,\"selfadapt\":true,\"oneoff\":false,\"alreport\":true,\"pos\":1,\"hb\":2}";

        EncoderResult result = encoder.encode(input(SET_CONFIG, command));

        assertTrue(result.isSuccess());
        assertEquals("95000102", result.getEncodedCommand());
    }

    @Test
    void shouldUseDefaultsForMissingSetConfigParameters() throws EncoderServiceException {
        EncoderResult result = encoder.encode(input(SET_CONFIG, "{}"));

        assertTrue(result.isSuccess());
        assertEquals("90000000", result.getEncodedCommand());
    }

    @Test
    void shouldUseDefaultsForPartialSetConfigParameters() throws EncoderServiceException {
        EncoderResult result = encoder.encode(input(SET_CONFIG, "{\"breakpoint\":true,\"hb\":5}"));

        assertTrue(result.isSuccess());
        assertEquals("98000005", result.getEncodedCommand());
    }

    @Test
    void shouldUseDefaultsForNullSetConfigParameters() throws EncoderServiceException {
        String command = "{\"breakpoint\":null,\"selfadapt\":null,\"oneoff\":null,\"alreport\":null,\"pos\":null,\"hb\":null}";

        EncoderResult result = encoder.encode(input(SET_CONFIG, command));

        assertTrue(result.isSuccess());
        assertEquals("90000000", result.getEncodedCommand());
    }

    @Test
    void shouldEncodeSimpleRequests() throws EncoderServiceException {
        assertEquals("A1FF", encoder.encode(input(POSITION_REQUEST, null)).getEncodedCommand());
        assertEquals("A2FF", encoder.encode(input(REGISTER_REQUEST, null)).getEncodedCommand());
        assertEquals("A3FF", encoder.encode(input(DEVICE_REQUEST, null)).getEncodedCommand());
    }

    @Test
    void shouldFailOnInvalidSetConfigJson() throws EncoderServiceException {
        EncoderResult result = encoder.encode(input(SET_CONFIG, "{invalid"));

        assertFalse(result.isSuccess());
        assertEquals("Encoding Payload Failed", result.getMessage());
    }

    @Test
    void shouldIgnoreOtherDevices() throws EncoderServiceException {
        Map<String, String> args = encoderArguments();
        args.put("deviceManufacturer", "Other");

        assertNull(encoder.encode(new LpwanEncoderInputData(GId.asGId("12345"), SET_CONFIG, "{}", args)));
    }

    private LpwanEncoderInputData input(String commandName, String commandData) {
        return new LpwanEncoderInputData(GId.asGId("12345"), commandName, commandData, encoderArguments());
    }

    private Map<String, String> encoderArguments() {
        Map<String, String> encoderArgs = new HashMap<>();
        encoderArgs.put("deviceManufacturer", "Lansitec");
        encoderArgs.put("deviceModel", "Asset Tracker");
        encoderArgs.put("sourceDeviceEui", "EUIID-12345");
        return encoderArgs;
    }
}
