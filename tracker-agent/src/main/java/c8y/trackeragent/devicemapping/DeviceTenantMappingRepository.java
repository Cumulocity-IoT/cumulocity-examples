package c8y.trackeragent.devicemapping;

import com.cumulocity.model.ID;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjects;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class DeviceTenantMappingRepository {

    private static final String TYPE = "c8y_device_tenant_Imei";
    static final String TENANT_FIELD = "c8y_tenantId";

    private final IdentityApi identityApi;
    private final InventoryApi inventoryApi;

    Optional<String> findTenantOptional(String externalDeviceId) {
        try {
            ExternalIDRepresentation externalId = identityApi.getExternalId(new ID(TYPE, externalDeviceId));
            return Optional.of((String) inventoryApi.get(externalId.getManagedObject().getId()).get(TENANT_FIELD));
        } catch (SDKException e) {
            if (e.getHttpStatus() == HttpStatus.SC_NOT_FOUND) {
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    Optional<ManagedObjectRepresentation> findManagedObjectForTenant(String tenant) {
        InventoryFilter filter = InventoryFilter.searchInventory()
                .byOwner("service_tracker-agent")
                .byFragmentType(TENANT_FIELD);
        for (ManagedObjectRepresentation t : inventoryApi.getManagedObjectsByFilter(filter).get().allPages()) {
            if (StringUtils.equals((String) t.get(TENANT_FIELD), tenant)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    ManagedObjectRepresentation createTenantObject(String tenant) {
        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        representation.set(tenant, TENANT_FIELD);
        return inventoryApi.create(representation);
    }

    void addDeviceToExistingTenant(String externalDeviceId, ManagedObjectRepresentation tenantObject) {
        identityApi.create(
                createExternalIDRepresentation(externalDeviceId, tenantObject)
        );
    }

    ExternalIDRepresentation createExternalIDRepresentation(String externalDeviceId, ManagedObjectRepresentation tenantObject) {
        ExternalIDRepresentation externalIDRepresentation = new ExternalIDRepresentation();
        externalIDRepresentation.setExternalId(externalDeviceId);
        externalIDRepresentation.setType(TYPE);
        externalIDRepresentation.setManagedObject(ManagedObjects.asManagedObject(tenantObject.getId()));
        return externalIDRepresentation;
    }
}
