package com.amlinv.activemq.monitor.web;

/**
 * Handler of websocket connections for the monitor update feed.
 *
 * Created by art on 4/22/14.
 */

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerListener;
import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.util.thread.DaemonThreadFactory;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.Path;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// TBD: make a resuable websocket class for use here and for AmqBridgeWebsocket
// TBD: hmm, need something better than static fields???
@Path("/ws/monitor")
@ServerEndpoint(value = "/ws/monitor")
public class MonitorWebsocket {
    private static final Logger         LOG = LoggerFactory.getLogger(MonitorWebsocket.class);

    private static final List<Session>        eventListeners = new ArrayList<Session>();

    private static ThreadPoolExecutor   executor =
                                        new ThreadPoolExecutor(
                                                1, 3, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                                                new DaemonThreadFactory("Monitor-Websocket-Dispatcher-Thread-"));

    private static Gson gson = new Gson();

    // TBD999: eliminate all the statics (have the websocket register itself as one of any number of listeners)
    // TBD999: process removals.  Probably use a registry of Queues and add a listener
    private static Map<String, Map<String, ActiveMQQueueStats>> queueStatsByBroker = new TreeMap<>();
    private static Map<String, ActiveMQQueueStats> queueStatsMap = new TreeMap<>();

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

    public static void  fireMonitorEvent(final String action, final String json) {
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

    protected static void onBrokerStatsUpdate (BrokerStatsPackage brokerStatsPackage) {
        ActiveMQBrokerStats brokerStats = brokerStatsPackage.getBrokerStats();
        Map<String, ActiveMQQueueStats> queueStats =
                Collections.unmodifiableMap(
                        new TreeMap<String, ActiveMQQueueStats>(brokerStatsPackage.getQueueStats()));

        synchronized ( queueStatsByBroker ) {
            queueStatsByBroker.put(brokerStats.getBrokerName(), queueStats);
        }

        // TBD: not every time (use a timer and/or check for all polled brokers reporting in)
        String brokerStatsJson = gson.toJson(brokerStatsPackage);
        fireMonitorEvent("brokerStats", brokerStatsJson);

        aggregateQueueStats();

        String queueStatsJson = gson.toJson(queueStatsMap);
        fireMonitorEvent("queueStats", queueStatsJson);
    }

    protected static void aggregateQueueStats () {
        // TBD: prevent high rate of calculation (perhaps perfom on schedule and only when updates detected)
        Map<String, ActiveMQQueueStats> newQueueStats = new TreeMap<>();

        synchronized ( queueStatsByBroker ) {
            for ( Map.Entry<String, Map<String, ActiveMQQueueStats>> statsforQueuesFromOneBroker :
                    queueStatsByBroker.entrySet() ) {
                Map<String, ActiveMQQueueStats> queueStats = statsforQueuesFromOneBroker.getValue();

                for ( Map.Entry<String, ActiveMQQueueStats> oneQueueStats : queueStats.entrySet() ) {
                    String queueName = oneQueueStats.getKey();
                    ActiveMQQueueStats newStats = oneQueueStats.getValue();

                    ActiveMQQueueStats base = newQueueStats.get(queueName);
                    ActiveMQQueueStats updated;
                    if ( base != null ) {
                        updated = base.add(newStats, "totals");
                    } else {
                        // TBD: after changing the poller to work with immutable copies, stop copying here
                        updated = newStats.copy("totals");
                    }

                    newQueueStats.put(queueName, updated);
                }
            }

            queueStatsMap = newQueueStats;
        }
    }

    protected static class MyBrokerPollerListener implements ActiveMQBrokerPollerListener {
        @Override
        public void onBrokerPollComplete (BrokerStatsPackage brokerStatsPackage) {
            onBrokerStatsUpdate(brokerStatsPackage);
        }
    }
}
