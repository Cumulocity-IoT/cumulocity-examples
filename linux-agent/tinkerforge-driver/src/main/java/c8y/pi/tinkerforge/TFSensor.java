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

package c8y.pi.tinkerforge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c8y.lx.driver.DeviceManagedObject;
import c8y.lx.driver.PollingDriver;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.tinkerforge.Device;

public abstract class TFSensor extends PollingDriver {
	public static final long DEFAULT_INTERVAL = 5000;

	public TFSensor(String id, Device device, String type, Object sensorFragment) {
		super(TFIds.getMeasurementType(type), TFIds.getPropertyName(type),
				DEFAULT_INTERVAL);
		this.id = id;
		this.device = device;
		sensorMo.set(sensorFragment);
	}

	@Override
	public void discoverChildren(ManagedObjectRepresentation parent) {
		setSource(sensorMo);
		DeviceManagedObject dmo = new DeviceManagedObject(getPlatform());
		try {
			dmo.createOrUpdate(sensorMo, TFIds.getXtId(id), parent.getId());
		} catch (SDKException e) {
			logger.warn("Cannot create sensor", e);
		}
	}

	protected ManagedObjectRepresentation getSensorMo() {
		return sensorMo;
	}

	protected Device getDevice() {
		return device;
	}

	protected Logger logger = LoggerFactory.getLogger(TFSensor.class);

	private Device device;
	private String id;
	private ManagedObjectRepresentation sensorMo = new ManagedObjectRepresentation();
}
