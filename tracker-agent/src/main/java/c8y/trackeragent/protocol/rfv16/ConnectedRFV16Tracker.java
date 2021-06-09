/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent.protocol.rfv16;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import c8y.trackeragent.protocol.TrackingProtocol;
import c8y.trackeragent.protocol.rfv16.parser.RFV16Fragment;
import c8y.trackeragent.tracker.BaseConnectedTracker;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConnectedRFV16Tracker extends BaseConnectedTracker<RFV16Fragment> {

    @Override
    public TrackingProtocol getTrackingProtocol() {
        return TrackingProtocol.RFV16;
    }
}
