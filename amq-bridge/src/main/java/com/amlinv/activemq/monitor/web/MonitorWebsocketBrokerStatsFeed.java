package com.amlinv.activemq.monitor.web;

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPollerListener;
import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.activemq.registry.DestinationRegistryListener;
import com.amlinv.activemq.registry.model.DestinationState;
import com.amlinv.activemq.stats.QueueStatisticsRegistry;
import com.amlinv.util.thread.DaemonThreadFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by art on 5/14/15.
 */
public class MonitorWebsocketBrokerStatsFeed implements ActiveMQBrokerPollerListener {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MonitorWebsocketBrokerStatsFeed.class);

    private Logger log = DEFAULT_LOGGER;

    private MonitorWebsocketRegistry websocketRegistry;

    private Gson gson = new GsonBuilder().create();

    private ScheduledThreadPoolExecutor executor =
            new ScheduledThreadPoolExecutor(
                    10, new DaemonThreadFactory("Monitor-Websocket-Dispatcher-Thread-"));

    private DestinationRegistryListener myQueueRegistryListener = new MyQueueRegistryListener();

    private QueueStatisticsRegistry queueStatisticsRegistry;

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

    public ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
    }

    public DestinationRegistryListener getQueueRegistryListener() {
        return myQueueRegistryListener;
    }

    public QueueStatisticsRegistry getQueueStatisticsRegistry() {
        return queueStatisticsRegistry;
    }

    public void setQueueStatisticsRegistry(QueueStatisticsRegistry queueStatisticsRegistry) {
        this.queueStatisticsRegistry = queueStatisticsRegistry;
    }

    public void init () {
    }

    @Override
    public void onBrokerPollComplete(BrokerStatsPackage brokerStatsPackage) {
        onBrokerStatsUpdate(brokerStatsPackage);
    }

    protected void onBrokerStatsUpdate (BrokerStatsPackage brokerStatsPackage) {
        ActiveMQBrokerStats brokerStats = brokerStatsPackage.getBrokerStats();

        String brokerName = brokerStats.getBrokerName();
        Map<String, ActiveMQQueueJmxStats> oldQueueStats;
        Map<String, ActiveMQQueueJmxStats> newQueueStats = new ConcurrentHashMap<>(brokerStatsPackage.getQueueStats());


        //
        // Update the metrics for the queues for which statistics were collected.
        //
        for ( ActiveMQQueueJmxStats brokerQueueStats : brokerStatsPackage.getQueueStats().values() ) {
            this.queueStatisticsRegistry.onUpdatedStats(brokerQueueStats);
        }

        String brokerStatsJson = gson.toJson(brokerStatsPackage);
        fireMonitorEvent("brokerStats", brokerStatsJson);

        // TBD: not every time (use a timer and/or check for all polled brokers reporting in)
        String queueStatsJson = gson.toJson(queueStatisticsRegistry.getQueueStats());
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

    /**
     * Listener for events on the queue registry.
     */
    protected class MyQueueRegistryListener implements DestinationRegistryListener {
        @Override
        public void onPutEntry(String putKey, DestinationState putValue) {
            String queueNameJson = gson.toJson(putValue.getName());

            fireMonitorEvent("queueAdded", queueNameJson);
        }

        @Override
        public void onRemoveEntry(String removeKey, DestinationState removeValue) {
            //
            // Notify the websocket listeners of the queue removal.
            //
            String queueNameJson = gson.toJson(removeValue.getName());

            fireMonitorEvent("queueRemoved", queueNameJson);
        }

        @Override
        public void onReplaceEntry(String replaceKey, DestinationState oldValue, DestinationState newValue) {
        }
    }
}
