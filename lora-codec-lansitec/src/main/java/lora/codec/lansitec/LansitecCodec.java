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

package lora.codec.lansitec;

import com.cumulocity.microservice.lpwan.codec.Codec;
import com.cumulocity.microservice.lpwan.codec.model.DeviceCommand;
import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import lora.codec.lansitec.encoder.LansitecEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class LansitecCodec implements Codec {

    /**
     * This method should populate a set of unique devices identified by their manufacturer and model.
     *
     * @return Set: A set of unique devices identified by their manufacturer and model.
     */
    public Set<DeviceInfo> supportsDevices() {

        // The manufacturer "LANSITEC" has 2 different devices with model "Outdoor Asset Tracker" and "Temperature Sensor"
        DeviceCommand positionRequestCommand = new DeviceCommand(LansitecEncoder.POSITION_REQUEST, "Device Config", LansitecEncoder.POSITION_REQUEST);
        DeviceCommand deviceRequestCommand = new DeviceCommand(LansitecEncoder.DEVICE_REQUEST, "Device Config", LansitecEncoder.DEVICE_REQUEST);
        DeviceCommand registerRequestCommand = new DeviceCommand(LansitecEncoder.REGISTER_REQUEST, "Device Config", LansitecEncoder.REGISTER_REQUEST);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode deviceOperationElements = mapper.createObjectNode();
        deviceOperationElements.put("breakpoint",Boolean.TRUE);
        deviceOperationElements.put("selfadapt",Boolean.TRUE);
        deviceOperationElements.put("oneoff",Boolean.TRUE);
        deviceOperationElements.put("alreport",Boolean.TRUE);
        deviceOperationElements.put("pos",0);
        deviceOperationElements.put("hb",0);
        String json =null;
        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(deviceOperationElements);
        } catch (JacksonException e) {
            e.printStackTrace();
        }
        DeviceCommand setConfigCommand = new DeviceCommand(LansitecEncoder.SET_CONFIG, "Device Config", json);

        Set<DeviceCommand> deviceCommands = new HashSet<>();
        deviceCommands.add(positionRequestCommand);
        deviceCommands.add(deviceRequestCommand);
        deviceCommands.add(registerRequestCommand);
        deviceCommands.add(setConfigCommand);

        Set<DeviceCommand> deviceCommands2 = new HashSet<>();
        deviceCommands2.add(positionRequestCommand);
        deviceCommands2.add(deviceRequestCommand);

        DeviceInfo deviceInfo_Lansitec_Asset_Tracker = new DeviceInfo("LANSITEC", "Asset Tracker", deviceCommands);
        DeviceInfo deviceInfo_Lansitec_Temperature_Sensor = new DeviceInfo("LANSITEC", "Temperature Sensor", deviceCommands2);

        return Stream.of(deviceInfo_Lansitec_Asset_Tracker, deviceInfo_Lansitec_Temperature_Sensor).collect(Collectors.toCollection(HashSet::new));
    }
}
