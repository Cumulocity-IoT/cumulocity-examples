/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.migration;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;

import c8y.trackeragent.devicebootstrap.DeviceCredentials;
import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import c8y.migration.model.TenantMigrationRequest;
import c8y.migration.model.TenantMigrationResponse;
import c8y.migration.service.MigrationRequestService;
import c8y.migration.service.MigrationResponseSerializer;
import c8y.migration.steps.MigrationStep;

@Component
public class Migrator {

	private static final Logger logger = LoggerFactory.getLogger(Migrator.class);

	private final MigrationRequestService requestService;
	private final ContextService<UserCredentials> contextService;
	private final Settings settings;
	private final List<MigrationStep> migrationSteps;

	@Autowired
	public Migrator(
			// @formatter:off
			MigrationRequestService requestService,
			ContextService<UserCredentials> contextService,
			Settings settings, 
			List<MigrationStep> migrationSteps) {
		// @formatter:on
		this.requestService = requestService;
		this.contextService = contextService;
		this.settings = settings;
		this.migrationSteps = migrationSteps;
	}

	@PostConstruct
	public void start() {
		List<TenantMigrationRequest> reqs = getMigrationRequests();
		List<TenantMigrationResponse> responses = new ArrayList<TenantMigrationResponse>();
		logger.info("Migrator requests: {}", reqs);
		for (TenantMigrationRequest req : reqs) {
			DeviceCredentials credentials = new DeviceCredentials(req.getTenant(), settings.getCepUser(),
					settings.getCepPassword(), null, null);
			contextService.runWithinContext(credentials, () -> {
				logger.info("Migrate tenant: {}", req.getTenant());
				TenantMigrationResponse response = migrateTenant(req);
				responses.add(response);
				logger.info("Migrate tenant: {} - DONE", req.getTenant());
			});
		}
		new MigrationResponseSerializer("device.properties").serialize(responses);
	}

	private List<TenantMigrationRequest> getMigrationRequests() {
		if (settings.getTenants() == null || settings.getTenants().isEmpty()) {
			return requestService.getAll();			
		} else {
			return requestService.getAll(settings.getTenants());
		}
	}

	private TenantMigrationResponse migrateTenant(TenantMigrationRequest req) {
		TenantMigrationResponse response = new TenantMigrationResponse(req.getTenant());
		for (MigrationStep migrationStep : migrationSteps) {
			logger.info("Step: {}", migrationStep.getClass().getSimpleName());
			migrationStep.execute(req, response);
			logger.info("Step: {} DONE", migrationStep.getClass().getSimpleName());
		}
		return response;
	}

}
