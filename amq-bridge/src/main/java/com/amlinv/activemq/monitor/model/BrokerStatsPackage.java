package com.amlinv.activemq.monitor.model;

import java.util.Map;

/**
 * Created by art on 4/11/15.
 */
public class BrokerStatsPackage {
    private final ActiveMQBrokerStats brokerStats;
    private final Map<String, ActiveMQQueueJmxStats> queueStats;

    public BrokerStatsPackage(ActiveMQBrokerStats brokerStats, Map<String, ActiveMQQueueJmxStats> queueStats) {
        this.brokerStats = brokerStats;
        this.queueStats = queueStats;
    }

    public ActiveMQBrokerStats getBrokerStats() {
        return brokerStats;
    }

    public Map<String, ActiveMQQueueJmxStats> getQueueStats() {
        return queueStats;
    }
}
