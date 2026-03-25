package c8y.example.mqttservice.client;

import c8y.IsDevice;
import com.cumulocity.model.Agent;
import com.cumulocity.model.ID;
import com.cumulocity.model.measurement.MeasurementValue;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class C8YClient {

    private final InventoryApi inventoryApi;

    private final IdentityApi identityApi;

    private final MeasurementApi measurementApi;

    private static final int IDENTITY_CACHE_SIZE = 1000;

    private final Map<ID, ExternalIDRepresentation> externalIdCache = Collections.synchronizedMap(new LinkedHashMap<ID, ExternalIDRepresentation>() {
        //Removing oldest entries
        @Override
        protected boolean removeEldestEntry(Map.Entry<ID, ExternalIDRepresentation> eldest) {
            return size() > IDENTITY_CACHE_SIZE;
        }
    });

    //FIXME: Proper Handling of updating Cache when External IDs are updated/deleted in Cumulocity, currently not handled, can lead to stale data in cache
    public ExternalIDRepresentation retrieveExternalId(String tenant, String type, String externalId) {
        try {
            ID id = new ID();
            id.setType(type);
            id.setValue(externalId);
            //Step 1: Check Cache to retrieve External ID
            if(externalIdCache.get(id) != null) {
                log.info("{} - External ID found in cache of type {} and value {}", tenant, type, externalId);
                return externalIdCache.get(id);
            } else {
                //Step 2: Cache miss, call API
                log.info("{} - External ID not found in cache of type {} and value {}, calling API", tenant, type, externalId);
                ExternalIDRepresentation extId = identityApi.getExternalId(id);
                externalIdCache.put(id, extId);
                return extId;
            }
        } catch (SDKException e) {
            log.info("{} - External ID could not be found of type {} and value {}", tenant, type, externalId);
        }
        return null;
    }

    public ExternalIDRepresentation createExternalId(String tenant, String type, String externalId, ManagedObjectRepresentation mor) {
        try {
            ExternalIDRepresentation extId = new ExternalIDRepresentation();
            extId.setType(Objects.requireNonNullElse(type, "c8y_Serial"));
            extId.setExternalId(externalId);
            extId.setManagedObject(mor);
            extId = identityApi.create(extId);
            ID id = new ID();
            id.setType(extId.getType());
            id.setValue(extId.getExternalId());
            //Adding External ID to Cache
            externalIdCache.put(id, extId);
            return extId;
        } catch (SDKException e) {
            log.error("{} - Error when creating external ID of type {} and value {}", tenant, type, externalId, e);
            throw e;
        }
    }

    public ManagedObjectRepresentation createDevice(String tenant, String name, String deviceId, String type, String extIdType) {
        try {
            ManagedObjectRepresentation mor = new ManagedObjectRepresentation();
            mor.setName(name);
            mor.set(new Agent());
            mor.setType(type);
            HashMap<String, String> agentFragments = new HashMap<>();
            agentFragments.put("name", "Example MQTT Service Microservice");
            agentFragments.put("version", "1.0.0");
            agentFragments.put("url", "https://github.com/Cumulocity-IoT/cumulocity-examples");
            agentFragments.put("maintainer", "Open-Source");
            mor.set(agentFragments, "c8y_Agent");
            mor.set(new IsDevice());
            mor = inventoryApi.create(mor);
            log.info("{} - New device created: {}", tenant, mor);
            return mor;
        } catch (SDKException e) {
            log.error("{} - Error when creating device with ID {}", tenant, deviceId, e);
            throw e;
        }
    }



    public MeasurementRepresentation createSimpleMeasurement(String tenant, ManagedObjectRepresentation mor, String name, String type, DateTime time, BigDecimal value, String unit) throws SDKException {
        MeasurementRepresentation measurementRepresentation = new MeasurementRepresentation();
        measurementRepresentation.setType(type);
        measurementRepresentation.setDateTime(time);
        measurementRepresentation.setSource(mor);
        MeasurementValue measurementValue = new MeasurementValue();
        HashMap<String, MeasurementValue> series = new HashMap<>();
        measurementValue.setValue(value);
        if (unit != null) {
            measurementValue.setUnit(unit);
        }
        series.put("T", measurementValue);
        measurementRepresentation.set(series, name);
        try {
            log.info("{} - Creating Measurement {}", tenant, measurementRepresentation.toJSON());
            return measurementApi.create(measurementRepresentation);
        } catch (SDKException e) {
            log.error("{} - Error when creating measurement {}", tenant, measurementRepresentation, e);
            throw e;
        }

    }

}
