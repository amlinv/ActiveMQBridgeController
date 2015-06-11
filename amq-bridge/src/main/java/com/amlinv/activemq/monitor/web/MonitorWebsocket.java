package com.amlinv.activemq.monitor.web;

/**
 * Handler of websocket connections for the monitor update feed.
 *
 * Created by art on 4/22/14.
 */

import com.amlinv.javasched.Scheduler;
import com.amlinv.javasched.Step;
import com.amlinv.javasched.process.StepListSchedulerProcess;
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
    public static final long MAX_MSG_BACKLOG = 100;

    private static MonitorWebsocketRegistry registry;
    private static long sendTimeout = DEFAULT_SEND_TIMEOUT;
    private static Scheduler scheduler;

    private Session socketSession;
    private String socketSessionId;

    private LinkedList<String> backlog = new LinkedList<>();

    private StepListSchedulerProcess sendProcess = new StepListSchedulerProcess();

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

    public static void setScheduler(Scheduler scheduler) {
        MonitorWebsocket.scheduler = scheduler;
    }

    public MonitorWebsocket() {
        scheduler.startProcess(this.sendProcess);
    }

    @OnClose
    public void onClose(Session sess, CloseReason reason) {
        LOG.info("Closed websocket session: sessionId={}; reason='{}'", sess.getId(), reason.toString());

        registry.remove(sess.getId());

        this.backlog.clear();
        this.sendProcess.shutdown();
    }

    @OnError
    public void onError (Session sess, Throwable thrown) {
        LOG.info("Error on websocket session: sessionId={}", sess.getId(), thrown);

        this.safeClose();
    }

    @OnOpen
    public void onOpen(Session sess) {
        LOG.debug("websocket connection open: sessionId={}", sess.getId());

        this.socketSession = sess;
        this.socketSessionId = sess.getId();
        this.socketSession.getAsyncRemote().setSendTimeout(sendTimeout);

        registry.put(sess.getId(), this);
    }

    @OnMessage
    public void onMessage (String msg, Session sess) {
        LOG.debug("message from client {}", msg);
    }

    public void fireMonitorEventNB(final String action, final String content) throws IOException {
        if (this.socketSession == null) {
            LOG.info("ignoring event; socket session is undefined: sessionId={}", this.socketSessionId);
            return;
        }

        String  msg = "{\"action\": \"" + action + "\", \"data\": " + content + "}";

        queueSendToWebsocketNB(msg);
    }

    /**
     * Send the given message to the
     * @param msg
     */
    protected void queueSendToWebsocketNB(String msg) {
        if (this.sendProcess.getPendingStepCount() < MAX_MSG_BACKLOG) {
            MySendStep sendStep = new MySendStep(msg);
            this.sendProcess.addStep(sendStep);
        } else {
            LOG.info("websocket backlog is full; aborting connection: sessionId={}", this.socketSessionId);

            this.safeClose();
        }
    }

    /**
     * Write the given message to the websocket now.
     *
     * @param msg text to send to the websocket.
     */
    private void writeToWebsocket(String msg) throws IOException {
        this.socketSession.getBasicRemote().sendText(msg);
    }


    /**
     * Safely close the websocket.
     */
    protected void safeClose () {
        Session closeSession = this.socketSession;
        this.socketSession = null;

        try {
            if (closeSession != null) {
                //
                // Remove from the registry first in case the close blocks for a while.  Doing so prevents additional
                // errors logged for attempts to send to this now-defunct websocket.
                //
                registry.remove(this.socketSessionId);
                closeSession.close();
            }
        } catch (IOException ioExc) {
            LOG.debug("io exception on safe close of session", ioExc);
        }
    }

    protected void onIoException(IOException ioExc) {
        this.LOG.info("IO exception on write to websocket; closing", ioExc);
        this.sendProcess.shutdown();
        this.safeClose();
    }

    protected class MySendStep implements Step {
        private MonitorWebsocket parent = MonitorWebsocket.this;

        private final String msg;

        public MySendStep(String msg) {
            this.msg = msg;
        }

        @Override
        public void execute() {
            try {
                parent.writeToWebsocket(msg);
            } catch ( IOException ioExc ) {
                parent.onIoException(ioExc);
            }
        }

        @Override
        public boolean isBlocking() {
            return true;
        }
    }
}
