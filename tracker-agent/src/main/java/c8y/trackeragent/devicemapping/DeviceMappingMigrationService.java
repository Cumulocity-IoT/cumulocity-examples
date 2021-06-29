package c8y.trackeragent.devicemapping;

import c8y.trackeragent.devicebootstrap.DeviceCredentials;
import c8y.trackeragent.devicebootstrap.DeviceCredentialsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DeviceMappingMigrationService {

    private final DeviceCredentialsRepository deviceCredentialsRepository;
    private final DeviceTenantMappingService deviceTenantMappingService;

    public void doMigrationFromPropertyFileToManagedObject() {
        for (DeviceCredentials device : deviceCredentialsRepository.getAllDeviceCredentials()) {
            deviceTenantMappingService.addDeviceToTenant(device.getImei(), device.getTenant());
        }
    }
}
