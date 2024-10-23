package c8y.remoteaccess.tunnel;

import c8y.remoteaccess.util.CumulocityIOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class WebSocketClient extends Endpoint implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

    private Session session;

    private final DeviceSocketClient deviceClient;

    public WebSocketClient(DeviceSocketClient deviceClient) {
        super();
        this.deviceClient = deviceClient;
    }

    public void sendMessage(ByteBuffer data) throws IOException {
        if (session != null && session.getBasicRemote() != null) {
            session.getBasicRemote().sendBinary(data);
        } else {
            logger.debug("Unable to send message. No basic remote!");
        }
    }

    @Override
    public void close() {
        logger.debug("Closing websocket.");
        CumulocityIOUtils.closeQuietly(session, deviceClient);
    }

    public void onOpen(Session session, EndpointConfig config) {
        logger.debug("Websocket was opened.");
        this.session = session;
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

            @Override
            public void onMessage(ByteBuffer message) {
                onBinaryMessage(message);
            }
        });
    }

    public void onBinaryMessage(ByteBuffer data) {
        logger.debug("Received {} bytes from websocket, Forwarding to device...", data.remaining());
        try {
            deviceClient.sendMessage(data.array());
        } catch (Exception e) {
            logger.error("Unable to forward message to device");
            try {
                deviceClient.close();
                close();
            } catch (IOException e1) {
                logger.debug("Exception when closing device client connection: ", e.getMessage());
            }
        }
    }

    public void onError(Session session, Throwable e) {
        logger.error("Websocket[{}] encountered an error:", session.getId(), e);
    }

    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("Websocket[{}] was closed.", session.getId());
        this.session = null;
    }

}
