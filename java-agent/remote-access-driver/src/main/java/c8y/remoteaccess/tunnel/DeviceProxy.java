package c8y.remoteaccess.tunnel;

import c8y.remoteaccess.RemoteAccessException;
import c8y.remoteaccess.RemoteAccessProtocolException;
import c8y.remoteaccess.RemoteAccessWebsocketException;
import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.PlatformParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;

public class DeviceProxy {

    private static final String WEBSOCKET_DEVICE_ENDPOINT = "/service/remoteaccess/device/";

    private static final Logger logger = LoggerFactory.getLogger(DeviceProxy.class);

    private String deviceHost;

    private int devicePort;

    private String websocketHost;

    private int websocketPort;

    private String connectionKey;

    private boolean encrypted;

    private TunnelingThread thread;

    private DeviceSocketClient deviceSocket;

    private String tenantUsername;

    private String password;

    private WebSocketClient websocket;

    public DeviceProxy(String hostname, int port, String connectionKey, Platform platform) throws MalformedURLException {

        if (!(platform instanceof PlatformParameters)) {
            throw new IllegalArgumentException("Unable to determine host by platform: " + platform);
        }

        PlatformParameters parameters = (PlatformParameters) platform;
        URL url = new URL(parameters.getHost());

        this.encrypted = url.getProtocol().equals("https");
        this.websocketHost = url.getHost();
        this.websocketPort = url.getPort();

        CumulocityBasicCredentials cumulocityBasicCredentials = (CumulocityBasicCredentials) parameters.getCumulocityCredentials();
        this.tenantUsername = cumulocityBasicCredentials.getLoginWithTenant();
        this.password = cumulocityBasicCredentials.getPassword();

        this.deviceHost = hostname;
        this.devicePort = port;
        this.connectionKey = connectionKey;
    }

    private URI getWebsocketEndpoint() {
        String protocol = (encrypted ? "wss://" : "ws://");
        String hostname = websocketHost + ((websocketPort > 0) ? (":" + websocketPort) : "");
        return URI.create(protocol + hostname + WEBSOCKET_DEVICE_ENDPOINT + connectionKey);
    }

    public void start() throws RemoteAccessException {

        logger.debug("DeviceProxy starting");

        try {
            URI endpoint = getWebsocketEndpoint();

            try {
                // Connect to Device
                logger.debug("Creating device connection " + deviceHost + ":" + devicePort);
                deviceSocket = new DeviceSocketClient(deviceHost, devicePort);
                deviceSocket.connect();
            } catch (IOException e) {
                logger.error("Device connect error: {}", e.getMessage());
                throw new RemoteAccessProtocolException(e.getMessage());
            }

            try {
                // Connect to websocket
                logger.debug("Creating websocket connection {}", endpoint);
                websocket = new WebSocketClient(deviceSocket);

                ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
                        .configurator(new AuthHeaderConfigurator(tenantUsername, password)).preferredSubprotocols(Collections.singletonList("binary"))
                        .build();
                ContainerProvider.getWebSocketContainer().connectToServer(websocket, endpointConfig, endpoint);
            } catch (IOException | DeploymentException e) {
                logger.error("Websocket connect error: {}", e.getMessage());
                throw new RemoteAccessWebsocketException(e.getMessage());
            }

            // Start tunneling thread
            logger.debug("Starting tunneling thread");
            thread = new TunnelingThread(websocket, deviceSocket);
            thread.start();

            logger.debug("Tunneling operational");

        } catch (RemoteAccessException e) {
            stop();
            throw e;
        }
    }

    public void stop() throws RemoteAccessException {
        logger.info("Stopping tunnel...");

        try {
            if (thread != null) {
                logger.debug("Stopping tunneling thread");
                thread.stop();
                thread = null;
            }

            if (deviceSocket != null) {
                logger.debug("Closing device connection");
                deviceSocket.close();
                deviceSocket = null;
            }

            if (websocket != null) {
                logger.debug("Closing websocket connection");
                websocket.close();
                websocket = null;
            }
        } catch (IOException e) {
            throw new RemoteAccessException(e.getMessage());
        }
    }
}
