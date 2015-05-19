package com.amlinv.activemq.monitor.web;

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerListener;
import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.activemq.registry.DestinationRegistryListener;
import com.amlinv.activemq.registry.model.DestinationInfo;
import com.amlinv.util.thread.DaemonThreadFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
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

    private DestinationRegistryListener myQueueRegistryListener = new MyQueueRegistryListener();

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // METHODS
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    public DestinationRegistryListener getQueueRegistryListener() {
        return myQueueRegistryListener;
    }

    @Override
    public void onBrokerPollComplete(BrokerStatsPackage brokerStatsPackage) {
        onBrokerStatsUpdate(brokerStatsPackage);
    }

    protected void onBrokerStatsUpdate (BrokerStatsPackage brokerStatsPackage) {
        ActiveMQBrokerStats brokerStats = brokerStatsPackage.getBrokerStats();
        Map<String, ActiveMQQueueStats> queueStats = new ConcurrentHashMap<>(brokerStatsPackage.getQueueStats());

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

    protected void fireMonitorEvent (final String action, final String content) {
        for ( final MonitorWebsocket oneTarget : this.websocketRegistry.values() ) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        oneTarget.fireMonitorEvent(action, content);
                    } catch (Throwable exc) {
                        log.info("error attempting to send event to listener", exc);
                    }
                }
            });
        }
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

    /**
     * Listener for events on the queue registry.
     */
    protected class MyQueueRegistryListener implements DestinationRegistryListener {
        @Override
        public void onPutEntry(String putKey, DestinationInfo putValue) {
            fireMonitorEvent("queueAdded", putValue.getName());
        }

        @Override
        public void onRemoveEntry(String removeKey, DestinationInfo removeValue) {
            //
            // Remove the statistics for the queue from all of the broker statistics.
            //
            synchronized ( queueStatsByBroker ) {
                for ( Map<String, ActiveMQQueueStats> queueStatsForBroker : queueStatsByBroker.values() ) {
                    queueStatsForBroker.remove(removeValue.getName());
                }
            }

            fireMonitorEvent("queueRemoved", removeValue.getName());
        }

        @Override
        public void onReplaceEntry(String replaceKey, DestinationInfo oldValue, DestinationInfo newValue) {
        }
    }
}
