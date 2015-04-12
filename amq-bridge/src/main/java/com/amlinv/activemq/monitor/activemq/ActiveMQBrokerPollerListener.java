package com.amlinv.activemq.monitor.activemq;

import com.amlinv.activemq.monitor.model.BrokerStatsPackage;

import java.util.Map;

/**
 * Created by art on 4/11/15.
 */
public interface ActiveMQBrokerPollerListener {
    void onBrokerPollComplete(BrokerStatsPackage brokerStatsPackage);
}
