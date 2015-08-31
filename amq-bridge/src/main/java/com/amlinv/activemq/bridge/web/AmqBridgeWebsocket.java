package com.amlinv.activemq.bridge.web;

/**
 * Handler of websocket connections for the bridge update feed.
 *
 * Created by art on 4/22/14.
 */

import com.amlinv.thread.util.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Path("/ws/bridges")
@ServerEndpoint(value = "/ws/bridges")
public class AmqBridgeWebsocket {
    private static final Logger         LOG = LoggerFactory.getLogger(AmqBridgeWebsocket.class);

    private static final List<Session>        eventListeners = new ArrayList<Session>();

    private static ThreadPoolExecutor   executor =
                                        new ThreadPoolExecutor(
                                                1, 3, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                                                new DaemonThreadFactory("Websocket-Dispatcher-Thread-"));

    @OnClose
    public void onClose(Session sess, CloseReason reason) {
        LOG.info("Closed websocket session: {}", reason.toString());
        synchronized ( eventListeners ) {
            eventListeners.remove(sess);
        }
    }

    @OnError
    public void onError (Session sess, Throwable thrown) {
        LOG.info("Error on websocket session", thrown);
    }

    @OnOpen
    public void onOpen (Session sess) {
        LOG.debug("websocket connection open");
        synchronized ( eventListeners ) {
            eventListeners.add(sess);
        }
    }

    @OnMessage
    public void onMessage (String msg, Session sess) {
        LOG.debug("message from client {}", msg);
    }

    public static void  fireBridgeEvent(final String action, final String json) {
        final List<Session> listeners;

        synchronized ( eventListeners ) {
            listeners = new ArrayList<Session>(eventListeners);
        }

        for ( final Session oneListener : listeners) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        String  msg = "{\"action\": \"" + action + "\", \"data\": " + json + "}";
                        oneListener.getBasicRemote().sendText(msg);
                    } catch (Throwable exc) {
                        LOG.info("error attempting to send event to listener", exc);
                    }
                }
            });
        }
    }
}
