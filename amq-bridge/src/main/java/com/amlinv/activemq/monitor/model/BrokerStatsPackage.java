package com.amlinv.activemq.monitor.model;

import java.util.Map;

/**
 * Created by art on 4/11/15.
 */
public class BrokerStatsPackage {
    private final ActiveMQBrokerStats brokerStats;
    private final Map<String, ActiveMQQueueStats> queueStats;

    public BrokerStatsPackage(ActiveMQBrokerStats brokerStats, Map<String, ActiveMQQueueStats> queueStats) {
        this.brokerStats = brokerStats;
        this.queueStats = queueStats;
    }

    public ActiveMQBrokerStats getBrokerStats() {
        return brokerStats;
    }

    public Map<String, ActiveMQQueueStats> getQueueStats() {
        return queueStats;
    }
}
