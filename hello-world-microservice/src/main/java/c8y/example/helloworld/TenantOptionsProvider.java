package c8y.example.helloworld;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.annotation.EnableContextSupport;
import com.cumulocity.microservice.context.credentials.UserCredentials;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@EnableContextSupport
@Service
public class TenantOptionsProvider {

    @Autowired
    private ContextService<UserCredentials> userContextService;

    @Autowired
    @Qualifier("userTenantOptionApi")
    private TenantOptionApi tenantOptionApi;

    private static final String CATEGORY = "OTLP";


    public List<OptionRepresentation> getTenantOptions() {
        UserCredentials userContext = new UserCredentials("management", "joe", "Cumulocity-1", null, null, null, null, "templates-scope");

        List<OptionRepresentation> options = new ArrayList<OptionRepresentation>();
        userContextService.runWithinContext(userContext, () -> {
            tenantOptionApi.getAllOptionsForCategory("OTLP").stream().toList().forEach(option -> {
                options.add(option);
            });
        });
        //return tenantOptionApi.getOption(optionPK);
        return options;
    }

}
