/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package c8y.lx.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;

import c8y.Configuration;
import c8y.lx.driver.Configurable;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.ManagedObject;

public class ConfigurationDriverTest {

	public static final String REFERENCE_CONFIG = "src/test/resources/configuration.txt";
	public static final String REFERENCE_PROPSTR = "propA=valueA\npropB=valueB\n";

	@Before
	public void setup() throws Exception {
		referenceProps.setProperty("propA", "valueA");
		referenceProps.setProperty("propB", "valueB");

		mo.setId(gid);
		op.setDeviceId(gid);
		op.set(new Configuration(REFERENCE_PROPSTR));

		Platform platform = mock(Platform.class);
		InventoryApi inventory = mock(InventoryApi.class);
		moHandle = mock(ManagedObject.class);

		when(platform.getInventoryApi()).thenReturn(inventory);
		when(inventory.getManagedObject(mo.getId())).thenReturn(moHandle);

		driver.initialize(platform);
	}

	@Test
	public void testFromString() throws IOException {
		Properties props = PropUtils.fromString(REFERENCE_PROPSTR);
		assertEquals(referenceProps, props);
	}

	@Test
	public void testToString() {
		String string = PropUtils.toString(referenceProps);
		string = string.substring(string.indexOf("\n") + 1);
		string = string.replace("\r", "");
		assertThat(string, new StringContains("propA=valueA"));
		assertThat(string, new StringContains("propB=valueB"));
	}

	@Test
	public void testFromFile() {
		Properties props = new Properties();
		PropUtils.fromFile(REFERENCE_CONFIG, props);
		assertEquals(referenceProps, props);
	}

	@Test
	public void testNonExistentFile() {
		Properties props = new Properties();
		PropUtils.fromFile("a non-existant file", props);
		assertEquals(new Properties(), props);
	}

	// TODO testToFile

	@Test
	public void testMergeDefaults() {
		// Test defaults
		driver.addConfigurable(cfgListener);
		assertEquals(2, driver.getProperties().size());
		assertEquals("valueD", driver.getProperties().getProperty("propB"));
		assertEquals("valueC", driver.getProperties().getProperty("propC"));

		// Test merged
		PropUtils.fromFile(REFERENCE_CONFIG, driver.getProperties());
		assertEquals(3, driver.getProperties().size());
		assertEquals("valueA", driver.getProperties().getProperty("propA"));
		assertEquals("valueB", driver.getProperties().getProperty("propB"));
		assertEquals("valueC", driver.getProperties().getProperty("propC"));
	}

	@Test
	public void testNotification() throws Exception {
		driver.discoverChildren(mo);
		driver.addConfigurable(cfgListener);
		driver.execute(op, false);
		assertTrue(cfgListener.isNotified());
	}

	@Test
	public void testInventoryUpdate() throws Exception {
		driver.discoverChildren(mo);
		driver.execute(op, false);

		ManagedObjectRepresentation mo = new ManagedObjectRepresentation();
		mo.setId(gid);
		mo.set(new Configuration(REFERENCE_PROPSTR));
		// verify(moHandle).update(mo);
		// TODO HONK ... ManagedObjectRepresentation has no equals method.
	}

	class TestConfigurationListener implements Configurable {
		@Override
		public void addDefaults(Properties props) {
			props.setProperty("propB", "valueD");
			props.setProperty("propC", "valueC");
		}

		@Override
		public void configurationChanged(Properties props) {
			notified = true;
		}

		public boolean isNotified() {
			return notified;
		}

		boolean notified = false;
	}

	private ConfigurationDriver driver = new ConfigurationDriver();
	private Properties referenceProps = new Properties();
	private GId gid = new GId("1");
	private ManagedObjectRepresentation mo = new ManagedObjectRepresentation();
	private OperationRepresentation op = new OperationRepresentation();
	private TestConfigurationListener cfgListener = new TestConfigurationListener();
	private ManagedObject moHandle;
}
