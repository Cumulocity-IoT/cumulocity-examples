/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.trackeragent;

import static com.cumulocity.rest.representation.tenant.OptionMediaType.OPTION;

import c8y.trackeragent.utils.TrackerPlatformProvider;
import com.cumulocity.microservice.context.inject.TenantScope;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.ResponseParser;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.SDKException;

import c8y.trackeragent.configuration.TrackerConfiguration;

import jakarta.ws.rs.core.Response;

@Component
@TenantScope
public class UpdateIntervalProvider {

	private final static Logger logger = LoggerFactory.getLogger(UpdateIntervalProvider.class);
	private final static String optionEndpoint = "tenant/system/options/device/update.interval";

	private final String path;
	private final TrackerPlatformProvider trackerPlatformProvider;

	@Autowired
	public UpdateIntervalProvider(TrackerConfiguration conf, TrackerPlatformProvider trackerPlatformProvider) {
		this.trackerPlatformProvider = trackerPlatformProvider;
		String host = conf.getPlatformHost();
		if (host == null) {
			throw new RuntimeException("Host cannot be null for options repository.");
		}
		if (host.charAt(host.length() - 1) != '/') {
            host = host + "/";
        }

		this.path = host + optionEndpoint;
		logger.info("Will use the following path to get update interval option: {}", path);
	}

	public Integer findUpdateInterval(String tenantId) {
		logger.info("Find update interval in tenant options.");
		try {
			OptionRepresentation option = trackerPlatformProvider.getTenantPlatform(tenantId)
					.rest()
					.get(path, OPTION, OptionRepresentation.class);

			logger.info("Update interval value is: {}", option.getValue());
			return Integer.parseInt(option.getValue());
		} catch (SDKException ex) {
			if (ex.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
				logger.info("Interval option not found.");
				return null;
			} else if (ex.getHttpStatus() == HttpStatus.SC_UNAUTHORIZED
					|| ex.getHttpStatus() == HttpStatus.SC_FORBIDDEN) {
				logger.info("Access to tenant options forbidden. User does not have access...");
				return null;
			} else {
				throw ex;
			}
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

}
