package c8y.remoteaccess.tunnel;

import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.ClientEndpointConfig;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AuthHeaderConfigurator extends ClientEndpointConfig.Configurator {

    private static final Logger logger = LoggerFactory.getLogger(AuthHeaderConfigurator.class);

    private String basicAuth;

    public AuthHeaderConfigurator(String username, String password) {
        this.basicAuth = BaseEncoding.base64().encode((username + ":" + password).getBytes());
    }

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        headers.put("Authorization", Collections.singletonList("Basic " + basicAuth));
        logger.debug("Headers: {}", headers);
        super.beforeRequest(headers);
    }
}
