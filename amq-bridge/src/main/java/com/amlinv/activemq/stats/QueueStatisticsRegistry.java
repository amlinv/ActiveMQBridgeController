package com.amlinv.activemq.stats;

import com.amlinv.activemq.monitor.model.ActiveMQQueueJmxStats;
import com.amlinv.activemq.registry.DestinationRegistryListener;
import com.amlinv.activemq.registry.model.DestinationState;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Registry of statistics for Queues, with details by broker.
 *
 * Created by art on 5/28/15.
 */
public class QueueStatisticsRegistry {
    private final Map<String, QueueStatisticsCollection> queueStats = new HashMap<>();

    private MyQueueRegistryListener queueRegistryListener = new MyQueueRegistryListener();

    public MyQueueRegistryListener getQueueRegistryListener() {
        return queueRegistryListener;
    }

    /**
     * Update the statistics in the registry given one set of polled statistics.  These statistics are assigned a
     * timestamp equal to "now".
     *
     * @param updatedStats statistics with which to update the registry.
     */
    public void onUpdatedStats(ActiveMQQueueJmxStats updatedStats) {
        QueueStatisticsCollection queueStatisticsCollection;

        synchronized ( this.queueStats ) {
            queueStatisticsCollection = this.queueStats.get(updatedStats.getQueueName());

            if ( queueStatisticsCollection == null ) {
                queueStatisticsCollection = new QueueStatisticsCollection(updatedStats.getQueueName());
                this.queueStats.put(updatedStats.getQueueName(), queueStatisticsCollection);
            }
        }

        queueStatisticsCollection.onUpdatedStats(updatedStats);
    }

    /**
     * Retrieve statistics for all of the queues.
     *
     * @return map of aggregated statistics for each queue in the registry.
     */
    public Map<String, ActiveMQQueueStats> getQueueStats() {
        Map<String, ActiveMQQueueStats> result;

        result = new TreeMap<>();

        synchronized ( this.queueStats ) {
            for (QueueStatisticsCollection queueStatisticsCollection : this.queueStats.values()) {
                result.put(queueStatisticsCollection.getQueueName(), queueStatisticsCollection.getQueueTotalStats());
            }
        }

        return result;
    }

    /**
     * Listener for events on the queue registry so we don't keep around statistics for queues that no longer exist.
     */
    protected class MyQueueRegistryListener implements DestinationRegistryListener {
        @Override
        public void onPutEntry(String putKey, DestinationState putValue) {
        }

        @Override
        public void onRemoveEntry(String removeKey, DestinationState removeValue) {
            //
            // Remove the statistics for the queue from all of the broker statistics.
            //
            synchronized ( queueStats ) {
                queueStats.remove(removeKey);
            }
        }

        @Override
        public void onReplaceEntry(String replaceKey, DestinationState oldValue, DestinationState newValue) {
        }
    }
}
