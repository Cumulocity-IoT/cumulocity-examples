/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
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

package com.cumulocity.agent.snmp.platform.pubsub.queue;

import com.cumulocity.agent.snmp.config.GatewayProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.cumulocity.agent.snmp.util.WorkspaceUtils.getWorkspacePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("MTM-55935")
@RunWith(MockitoJUnitRunner.class)
public class MeasurementQueueTest {

    @Mock
    private GatewayProperties gatewayProperties;

    private MeasurementQueue measurementQueue;

    private Path persistentFolderPath;


    @Before
    public void setUp() {
        Mockito.when(gatewayProperties.getGatewayIdentifier()).thenReturn(this.getClass().getSimpleName());
        Mockito.when(gatewayProperties.getGatewayDatabaseBaseDir()).thenReturn(getWorkspacePath());

        persistentFolderPath = Paths.get(
                getWorkspacePath(),
                ".snmp",
                gatewayProperties.getGatewayIdentifier().toLowerCase(),
                "chronicle",
                "queues",
                "MEASUREMENT".toLowerCase());

        clearParentFolder();

        measurementQueue = new MeasurementQueue(gatewayProperties);
    }

    @After
    public void tearDown() {
        if(measurementQueue != null) {
            measurementQueue.close();
        }

        clearParentFolder();
    }

    private void clearParentFolder() {
        if(persistentFolderPath.toFile().exists()) {
            try {
                Files.list(persistentFolderPath).forEach(fileInTheFolder -> fileInTheFolder.toFile().delete());
            } catch (IOException e) {
            }
        }
    }

    @Test
    public void shouldCreateQueueWithCorrectName() {
        assertEquals("MEASUREMENT", measurementQueue.getName());
    }

    @Test
    public void shouldCreateRequiredPersistenceFolder() {
        assertTrue(measurementQueue.getPersistenceFolder().exists());
        assertEquals(persistentFolderPath.toString(), measurementQueue.getPersistenceFolder().getPath());
    }
}