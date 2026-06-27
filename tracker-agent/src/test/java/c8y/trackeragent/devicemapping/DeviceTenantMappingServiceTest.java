package c8y.trackeragent.devicemapping;

import c8y.MicroserviceSubscriptionsServiceMock;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectCollectionRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.ManagedObjectCollection;
import com.cumulocity.sdk.client.inventory.PagedManagedObjectCollectionRepresentation;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTenantMappingServiceTest {

    private static final String TYPE = "c8y_device_tenant_Imei";
    private static final String TENANT_FIELD = "c8y_tenantId";

    private static final String OWNER_TENANT = "ownerTenant";

    private static final String MY_TENANT = "myTenant";
    private static final String MY_TENANT_ID = "abc123";
    private static final String OTHER_TENANT = "otherTenant";
    private static final String OTHER_TENANT_ID = "cba321";

    private static final String IMEI = "imei_1";

    private final IdentityApi identityApi = Mockito.mock(IdentityApi.class);
    private final InventoryApi inventoryApi = Mockito.mock(InventoryApi.class);

    private final DeviceTenantMappingRepository deviceTenantMappingRepository = new DeviceTenantMappingRepository(
            identityApi, inventoryApi
    );

    private final MicroserviceSubscriptionsService microserviceSubscriptionsService = new MicroserviceSubscriptionsServiceMock();

    private final DeviceTenantMappingService deviceTenantMappingService = new DeviceTenantMappingService(
            deviceTenantMappingRepository, microserviceSubscriptionsService, OWNER_TENANT
    );

    @BeforeEach
    void before() {
        Mockito.reset(identityApi);
        Mockito.reset(inventoryApi);
    }

    @Test
    void shouldFindTenantCorrectly() {
        //given
        ID tenantObjectId = new ID(TYPE, IMEI);
        ExternalIDRepresentation externalIDRepresentation = createExternalIDRepresentation(new GId(TYPE, IMEI, null));
        Mockito.when(identityApi.getExternalId(tenantObjectId))
                .thenReturn(externalIDRepresentation);
        ManagedObjectRepresentation tenantRepresentation = createRepresentationFromTenant(MY_TENANT, "aaa1");
        Mockito.when(inventoryApi.get(new GId(TYPE, IMEI, null)))
                .thenReturn(tenantRepresentation);

        //when
        String resultTenant = deviceTenantMappingService.findTenant(IMEI);

        //then
        assertThat(resultTenant).isEqualTo(MY_TENANT);
    }

    @Test
    void shouldNotFoundTenantBecauseOfIdentityApiThrowIssue() {
        //given
        ID tenantObjectId = new ID(TYPE, IMEI);
        Mockito.when(identityApi.getExternalId(tenantObjectId))
                .thenThrow(new SDKException("Some exception"));

        //when
        Assertions.assertThrows(RuntimeException.class, () -> {
            deviceTenantMappingService.findTenant(IMEI);
        });
    }

    @Test
    void shouldNotFoundTenantBecauseOfInventoryApiThrowIssue() {
        //given
        ID tenantObjectId = new ID(TYPE, IMEI);
        ExternalIDRepresentation externalIDRepresentation = createExternalIDRepresentation(new GId(TYPE, IMEI, null));
        Mockito.when(identityApi.getExternalId(tenantObjectId))
                .thenReturn(externalIDRepresentation);
        Mockito.when(inventoryApi.get(new GId(TYPE, IMEI, null)))
                .thenThrow(new SDKException("Some exception"));

        //when
        Assertions.assertThrows(RuntimeException.class, () -> {
            deviceTenantMappingService.findTenant(IMEI);
        });
    }

    @Test
    void shouldCreateTenantObjectAndAddExternalId() {
        //given
        ManagedObjectCollection managedObjectCollection = createEmptyManagedObjectCollection();
        Mockito.when(inventoryApi.getManagedObjectsByFilter(Mockito.any()))
                .thenReturn(managedObjectCollection);
        Mockito.when(inventoryApi.create(Mockito.any()))
                .thenReturn(createRepresentationFromTenant(MY_TENANT, MY_TENANT_ID));
        Mockito.when(identityApi.getExternalId(new ID(TYPE, IMEI)))
                .thenThrow(new SDKException(HttpStatus.SC_NOT_FOUND, "Not found"));

        //when
        deviceTenantMappingService.addDeviceToTenant(IMEI, MY_TENANT);

        //then
        ArgumentCaptor<ManagedObjectRepresentation> tenantArgument = ArgumentCaptor.forClass(ManagedObjectRepresentation.class);
        Mockito.verify(inventoryApi).create(tenantArgument.capture());
        ManagedObjectRepresentation representationValue = tenantArgument.getValue();
        assertThat(representationValue.get(TENANT_FIELD)).isEqualTo(MY_TENANT);

        ArgumentCaptor<ExternalIDRepresentation> identityArgument = ArgumentCaptor.forClass(ExternalIDRepresentation.class);
        Mockito.verify(identityApi).create(identityArgument.capture());
        ExternalIDRepresentation idRepresentation = identityArgument.getValue();
        assertThat(idRepresentation.getExternalId()).isEqualTo(IMEI);
        assertThat(idRepresentation.getType()).isEqualTo(TYPE);
        assertThat(idRepresentation.getManagedObject().getId().getValue()).isEqualTo(MY_TENANT_ID);
    }

    @Test
    void shouldAddExternalIdToExistingTenantObject() {
        //given
        ManagedObjectCollection managedObjectCollection = createManagedObjectCollectionWithTenants();
        Mockito.when(inventoryApi.getManagedObjectsByFilter(Mockito.any()))
                .thenReturn(managedObjectCollection);
        Mockito.when(identityApi.getExternalId(new ID(TYPE, IMEI)))
                .thenThrow(new SDKException(HttpStatus.SC_NOT_FOUND, "Not found"));

        //when
        deviceTenantMappingService.addDeviceToTenant(IMEI, MY_TENANT);

        //then
        Mockito.verify(inventoryApi, Mockito.never()).create(Mockito.any());

        ArgumentCaptor<ExternalIDRepresentation> identityArgument = ArgumentCaptor.forClass(ExternalIDRepresentation.class);
        Mockito.verify(identityApi).create(identityArgument.capture());
        ExternalIDRepresentation idRepresentation = identityArgument.getValue();
        assertThat(idRepresentation.getExternalId()).isEqualTo(IMEI);
        assertThat(idRepresentation.getType()).isEqualTo(TYPE);
        assertThat(idRepresentation.getManagedObject().getId().getValue()).isEqualTo(MY_TENANT_ID);
    }

    @Test
    void shouldNotAddExternalIdBecauseCorrectMappingAlreadyExists() {
        //given
        ManagedObjectCollection managedObjectCollection = createManagedObjectCollectionWithTenants();
        Mockito.when(inventoryApi.getManagedObjectsByFilter(Mockito.any()))
                .thenReturn(managedObjectCollection);
        Mockito.when(identityApi.getExternalId(new ID(TYPE, IMEI)))
                .thenReturn(createExternalIDRepresentation(new GId(TYPE, IMEI, null)));
        Mockito.when(inventoryApi.get(Mockito.any()))
                .thenReturn(createRepresentationFromTenant(MY_TENANT, MY_TENANT_ID));

        //when
        deviceTenantMappingService.addDeviceToTenant(IMEI, MY_TENANT);

        //then
        Mockito.verify(inventoryApi, Mockito.never()).create(Mockito.any());
        Mockito.verify(identityApi, Mockito.never()).create(Mockito.any());
    }

    @Test
    void shouldRemoveExternalIdFromOneTenantAndAddExternalIdToOther() {
        //given
        ManagedObjectCollection managedObjectCollection = createManagedObjectCollectionWithTenants();
        Mockito.when(inventoryApi.getManagedObjectsByFilter(Mockito.any()))
                .thenReturn(managedObjectCollection);
        Mockito.when(identityApi.getExternalId(new ID(TYPE, IMEI)))
                .thenReturn(createExternalIDRepresentation(new GId(TYPE, IMEI, null)));
        Mockito.when(inventoryApi.get(Mockito.any()))
                .thenReturn(createRepresentationFromTenant(OTHER_TENANT, OTHER_TENANT_ID));

        //when
        deviceTenantMappingService.addDeviceToTenant(IMEI, MY_TENANT);

        //then
        Mockito.verify(inventoryApi, Mockito.never()).create(Mockito.any());
        Mockito.verify(identityApi, Mockito.never()).create(Mockito.any());
    }

    private ExternalIDRepresentation createExternalIDRepresentation(GId tenantObjectId) {
        ExternalIDRepresentation externalIDRepresentation = new ExternalIDRepresentation();
        ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();
        managedObject.setId(tenantObjectId);
        externalIDRepresentation.setManagedObject(managedObject);
        return externalIDRepresentation;
    }

    private ManagedObjectCollection createEmptyManagedObjectCollection() {
        ManagedObjectCollection managedObjectCollection = Mockito.mock(ManagedObjectCollection.class);
        Mockito.when(managedObjectCollection.get()).thenReturn(new PagedManagedObjectCollectionRepresentation(new ManagedObjectCollectionRepresentation(), null));
        return managedObjectCollection;
    }

    private ManagedObjectCollection createManagedObjectCollectionWithTenants() {
        ManagedObjectCollectionRepresentation collection = new ManagedObjectCollectionRepresentation();
        ManagedObjectRepresentation myTenantRepresentation = new ManagedObjectRepresentation();
        myTenantRepresentation.setId(GId.asGId(MY_TENANT_ID));
        myTenantRepresentation.set(MY_TENANT, TENANT_FIELD);
        ManagedObjectRepresentation otherTenantRepresentation = new ManagedObjectRepresentation();
        otherTenantRepresentation.setId(GId.asGId(OTHER_TENANT_ID));
        otherTenantRepresentation.set(OTHER_TENANT, TENANT_FIELD);
        collection.setManagedObjects(Arrays.asList(
                myTenantRepresentation, otherTenantRepresentation
        ));
        ManagedObjectCollection managedObjectCollection = Mockito.mock(ManagedObjectCollection.class);
        Mockito.when(managedObjectCollection.get()).thenReturn(new PagedManagedObjectCollectionRepresentation(collection, null));
        return managedObjectCollection;
    }

    private ManagedObjectRepresentation createRepresentationFromTenant(String tenant, String id) {
        ManagedObjectRepresentation representation = new ManagedObjectRepresentation();
        representation.set(tenant, TENANT_FIELD);
        representation.setId(GId.asGId(id));
        return representation;
    }
}