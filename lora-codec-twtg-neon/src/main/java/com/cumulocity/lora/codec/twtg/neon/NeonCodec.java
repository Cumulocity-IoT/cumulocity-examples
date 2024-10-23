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

package com.cumulocity.lora.codec.twtg.neon;

import com.cumulocity.microservice.lpwan.codec.Codec;
import com.cumulocity.microservice.lpwan.codec.model.DeviceCommand;
import com.cumulocity.microservice.lpwan.codec.model.DeviceInfo;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class NeonCodec implements Codec {
    @Override
    public @NotNull @NotEmpty Set<DeviceInfo> supportsDevices() {
        String applicationConfigurationCommandTemplate =
                "{\n" +
                "\t\"header\": {\n" +
                "\t\t\"message_type\": \"application_configuration\",\n" +
                "\t\t\"protocol_version\": 2\n" +
                "\t},\n" +
                "\t\"device_type\": \"ts\",\n" +
                "\t\"temperature_measurement_interval__seconds\": 900,\n" +
                "\t\"periodic_event_message_interval\": 16,\n" +
                "\t\"events\": [{\n" +
                "\t\t\t\"mode\": \"above\",\n" +
                "\t\t\t\"threshold_temperature\": 80,\n" +
                "\t\t\t\"measurements_window\": 1\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"mode\": \"below\",\n" +
                "\t\t\t\"threshold_temperature\": -40,\n" +
                "\t\t\t\"measurements_window\": 1\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"mode\": \"increasing\",\n" +
                "\t\t\t\"threshold_temperature\": 10,\n" +
                "\t\t\t\"measurements_window\": 1\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"mode\": \"decreasing\",\n" +
                "\t\t\t\"threshold_temperature\": -10,\n" +
                "\t\t\t\"measurements_window\": 1\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";
        DeviceCommand applicationConfigurationCommand = new DeviceCommand("Application Configuration", "Application Configuration", applicationConfigurationCommandTemplate);

        String deviceConfigurationCommandTemplate =
                "{\n" +
                "\t\"header\": {\n" +
                "\t\t\"message_type\": \"device_configuration\",\n" +
                "\t\t\"protocol_version\": 2\n" +
                "\t},\n" +
                "\t\"switch_mask\": {\n" +
                "\t\t\"enable_confirmed_changed_message\": true\n" +
                "\t},\n" +
                "\t\"communication_max_retries\": 3,\n" +
                "\t\"unconfirmed_repeat\": 2,\n" +
                "\t\"periodic_message_random_delay_seconds\": 60,\n" +
                "\t\"status_message_interval_seconds\": 86400,\n" +
                "\t\"status_message_confirmed_interval\": 1,\n" +
                "\t\"lora_failure_holdoff_count\": 2,\n" +
                "\t\"lora_system_recover_count\": 1,\n" +
                "\t\"lorawan_fsb_mask\": [\n" +
                "\t\t\"0x00FF\",\n" +
                "\t\t\"0x0000\",\n" +
                "\t\t\"0x0000\",\n" +
                "\t\t\"0x0000\",\n" +
                "\t\t\"0x0000\"\n" +
                "\t]\n" +
                "}";
        DeviceCommand deviceConfigurationCommand = new DeviceCommand("Device Configuration", "Device Configuration", deviceConfigurationCommandTemplate);

        HashSet<DeviceCommand> supportedCommands = Stream.of(applicationConfigurationCommand, deviceConfigurationCommand).collect(Collectors.toCollection(HashSet::new));

        DeviceInfo neonTemperatureSensor = new DeviceInfo("TWTG", "Neon Temperature Sensor", supportedCommands);

        return Stream.of(neonTemperatureSensor).collect(Collectors.toCollection(HashSet::new));
    }
}
