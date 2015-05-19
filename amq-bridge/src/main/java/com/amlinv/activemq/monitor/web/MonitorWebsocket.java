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
import java.util.LinkedList;

// TBD: make a resuable websocket class for use here and for AmqBridgeWebsocket
@Path("/ws/monitor")
@ServerEndpoint(value = "/ws/monitor")
public class MonitorWebsocket {
    private static final Logger         LOG = LoggerFactory.getLogger(MonitorWebsocket.class);

    public static final long DEFAULT_SEND_TIMEOUT = 30000;
    public static final long MAX_MSG_BACKLOG = 20;

    private static MonitorWebsocketRegistry registry;
    private static long sendTimeout = DEFAULT_SEND_TIMEOUT;

    private Session socketSession;
    private final Object socketSessionSync = new Object();

    private boolean sendActive;
    private MySendHandler mySendHandler;
    private LinkedList<String> backlog = new LinkedList<>();

    public static MonitorWebsocketRegistry getRegistry() {
        return registry;
    }

    public static void setRegistry(MonitorWebsocketRegistry registry) {
        MonitorWebsocket.registry = registry;
    }

    public static long getSendTimeout() {
        return sendTimeout;
    }

    public static void setSendTimeout(long newSendTimeout) {
        sendTimeout = newSendTimeout;
    }

    @OnClose
    public void onClose(Session sess, CloseReason reason) {
        LOG.info("Closed websocket session: {}", reason.toString());

        registry.remove(sess.getId());

        this.backlog.clear();
    }

    @OnError
    public void onError (Session sess, Throwable thrown) {
        LOG.info("Error on websocket session", thrown);
    }

    @OnOpen
    public void onOpen(Session sess) {
        LOG.debug("websocket connection open");
        this.socketSession = sess;
        this.socketSession.getAsyncRemote().setSendTimeout(sendTimeout);

        registry.put(sess.getId(), this);
    }

    @OnMessage
    public void onMessage (String msg, Session sess) {
        LOG.debug("message from client {}", msg);
    }

    public void  fireMonitorEvent(final String action, final String content) throws IOException {
        String  msg = "{\"action\": \"" + action + "\", \"data\": " + content + "}";

        queueSendToWebsocket(msg);
    }

    /**
     * Send the given message to the
     * @param msg
     */
    protected void queueSendToWebsocket(String msg) {
        synchronized ( this.socketSessionSync ) {
            if ( this.socketSession != null ) {
                if ( this.sendActive ) {
                    if ( this.backlog.size() >= MAX_MSG_BACKLOG ) {
                        LOG.info("websocket backlog is full; aborting connection: sessionId={}",
                                this.socketSession.getId());

                        this.safeClose();
                        throw new RuntimeException("websocket message backlog is full; aborting websocket connection");
                    }

                    this.backlog.add(msg);
                } else {
                    //
                    // Create the send handler now if it hasn't yet been created.
                    //
                    if ( this.mySendHandler == null ) {
                        this.mySendHandler = new MySendHandler(this.socketSession.getId());
                    }

                    writeToWebsocket(msg, this.mySendHandler);
                }
            }
        }
    }

    /**
     * Write the given message to the websocket now using the send handler provided.
     *
     * @param msg text to send to the websocket.
     * @param sendHandler send handler to use with this write.
     */
    private void writeToWebsocket(String msg, SendHandler sendHandler) {
        this.sendActive = true;
        this.socketSession.getAsyncRemote().sendText(msg, sendHandler);
    }


    /**
     * Safely close the websocket.
     */
    protected void safeClose () {
        Session closeSession = this.socketSession;
        this.socketSession = null;

        try {
            closeSession.close();
        } catch (IOException ioExc) {
            LOG.debug("io exception on safe close of session", ioExc);
        }
    }

    /**
     * Send handler which receives notification of completed send to the websocket.
     */
    protected class MySendHandler implements SendHandler {
        private final String sessionId;

        public MySendHandler(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public void onResult(SendResult result) {
            //
            // Log the result.
            //
            if ( result.isOK() ) {
                LOG.trace("websocket session finished send: sessionId={}", this.sessionId);
            } else {
                LOG.info("exception on websocket send: sessionId={}", this.sessionId, result.getException());
            }

            //
            // If there is a backlog, trigger the next iteration and simply reuse this send handler.
            //
            synchronized ( socketSessionSync ) {
                sendActive = false;
                if ( backlog.size() > 0 ) {
                    writeToWebsocket(backlog.removeLast(), this);
                }
            }
        }
    };
}
