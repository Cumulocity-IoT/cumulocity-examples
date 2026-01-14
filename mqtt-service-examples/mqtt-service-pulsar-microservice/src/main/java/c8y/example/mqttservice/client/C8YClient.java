package c8y.example.mqttservice.client;

import c8y.IsDevice;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
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
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;

@Service
@Slf4j
public class C8YClient {

    @Autowired
    MicroserviceSubscriptionsService subscriptionsService;

    @Autowired
    InventoryApi inventoryApi;

    @Autowired
    IdentityApi identityApi;

    @Autowired
    MeasurementApi measurementApi;

    //FIXME: This method should use a Cache instead of calling on every message retrieved the API
    public ExternalIDRepresentation retrieveExternalId(String tenant, String type, String externalId) {
        try {
            ID id = new ID();
            id.setType(type);
            id.setValue(externalId);
            return identityApi.getExternalId(id);
        } catch (SDKException e) {
            log.info("{} - External ID could not be found of type {} and value {}", tenant, type, externalId);
        }
        return null;
    }

    public ManagedObjectRepresentation createDevice(String tenant, String name, String deviceId, String type) {
        try {
            ManagedObjectRepresentation mor = new ManagedObjectRepresentation();
            mor.setName(name);
            mor.set(new Agent());
            HashMap<String, String> agentFragments = new HashMap<>();
            agentFragments.put("name", "Example MQTT Service Microservice");
            agentFragments.put("version", "1.0.0");
            agentFragments.put("url", "https://github.com/Cumulocity-IoT/cumulocity-examples");
            agentFragments.put("maintainer", "Open-Source");
            mor.set(agentFragments, "c8y_Agent");
            mor.set(new IsDevice());
            mor = inventoryApi.create(mor);
            log.info("{} - New device created: {}", tenant, mor);
            ExternalIDRepresentation extId = new ExternalIDRepresentation();
            if(type != null)
                extId.setType(type);
            else
                extId.setType("c8y_Serial");
            extId.setExternalId(deviceId);
            extId.setManagedObject(mor);
            identityApi.create(extId);
            return mor;
        } catch (SDKException e) {
            log.error("{} - Error when creating device with ID {}", tenant, deviceId, e);
        }
        return null;
    }

    public MeasurementRepresentation createSimpleMeasurement(String tenant, ManagedObjectRepresentation mor, String type, DateTime time, BigDecimal value, String unit) {
        MeasurementRepresentation measurementRepresentation = new MeasurementRepresentation();
        try {
            measurementRepresentation.setType(type);
            measurementRepresentation.setDateTime(time);
            measurementRepresentation.setSource(mor);
            MeasurementValue measurementValue = new MeasurementValue();
            measurementValue.setValue(value);
            if (unit != null)
                measurementValue.setUnit(unit);
            measurementRepresentation.set(measurementValue);
            log.info("{} - Creating Measurement {}", tenant, measurementRepresentation.toJSON());
            return measurementApi.create(measurementRepresentation);
        } catch (SDKException e) {
            log.error("{} - Error when creating measurement {}", tenant, measurementRepresentation.toJSON());
        }
        return null;
    }

}
