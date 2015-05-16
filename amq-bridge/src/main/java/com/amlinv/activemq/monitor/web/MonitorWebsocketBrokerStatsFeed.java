package com.amlinv.activemq.monitor.web;

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerListener;
import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.util.thread.DaemonThreadFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by art on 5/14/15.
 */
public class MonitorWebsocketBrokerStatsFeed implements ActiveMQBrokerPollerListener {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MonitorWebsocketBrokerStatsFeed.class);

    private Logger log = DEFAULT_LOGGER;

    private MonitorWebsocketRegistry websocketRegistry;

    private Map<String, Map<String, ActiveMQQueueStats>> queueStatsByBroker = new TreeMap<>();
    private Map<String, ActiveMQQueueStats> queueStatsMap = new TreeMap<>();

    private Gson gson = new GsonBuilder().create();

    private ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    10, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    new DaemonThreadFactory("Monitor-Websocket-Dispatcher-Thread-"));


    public MonitorWebsocketRegistry getWebsocketRegistry() {
        return websocketRegistry;
    }

    public void setWebsocketRegistry(MonitorWebsocketRegistry websocketRegistry) {
        this.websocketRegistry = websocketRegistry;
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void onBrokerPollComplete(BrokerStatsPackage brokerStatsPackage) {
        onBrokerStatsUpdate(brokerStatsPackage);
    }

    protected void onBrokerStatsUpdate (BrokerStatsPackage brokerStatsPackage) {
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

    protected void fireMonitorEvent (final String action, final String json) {
        for ( final MonitorWebsocket oneTarget : this.websocketRegistry.values() ) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        oneTarget.fireMonitorEvent(action, json);
                    } catch (Throwable exc) {
                        log.info("error attempting to send event to listener", exc);
                    }
                }
            });
        }
        final List<Session> listeners;

    }

    protected void aggregateQueueStats () {
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

}
