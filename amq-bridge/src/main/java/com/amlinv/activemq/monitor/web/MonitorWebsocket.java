package com.amlinv.activemq.monitor.web;

/**
 * Handler of websocket connections for the monitor update feed.
 *
 * Created by art on 4/22/14.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.Path;
import java.io.IOException;

// TBD: make a resuable websocket class for use here and for AmqBridgeWebsocket
@Path("/ws/monitor")
@ServerEndpoint(value = "/ws/monitor")
public class MonitorWebsocket {
    private static final Logger         LOG = LoggerFactory.getLogger(MonitorWebsocket.class);

    private static MonitorWebsocketRegistry registry;

    private Session socketSession;

    public static MonitorWebsocketRegistry getRegistry() {
        return registry;
    }

    public static void setRegistry(MonitorWebsocketRegistry registry) {
        MonitorWebsocket.registry = registry;
    }


    @OnClose
    public void onClose(Session sess, CloseReason reason) {
        LOG.info("Closed websocket session: {}", reason.toString());

        registry.remove(sess.getId());
    }

    @OnError
    public void onError (Session sess, Throwable thrown) {
        LOG.info("Error on websocket session", thrown);
    }

    @OnOpen
    public void onOpen(Session sess) {
        LOG.debug("websocket connection open");
        this.socketSession = sess;

        registry.put(sess.getId(), this);
    }

    @OnMessage
    public void onMessage (String msg, Session sess) {
        LOG.debug("message from client {}", msg);
    }

    public void  fireMonitorEvent(final String action, final String json) throws IOException {
        String  msg = "{\"action\": \"" + action + "\", \"data\": " + json + "}";

        if ( this.socketSession != null ) {
            synchronized ( this.socketSession ) {
                this.socketSession.getBasicRemote().sendText(msg);
            }
        }
    }
}
