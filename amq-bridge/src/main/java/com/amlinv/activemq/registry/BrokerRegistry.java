package com.amlinv.activemq.registry;

import com.amlinv.activemq.registry.model.BrokerInfo;

import java.util.Set;

/**
 * Created by art on 5/2/15.
 */
public interface BrokerRegistry {
    boolean addBroker(BrokerInfo newBrokerInfo);
    BrokerInfo lookupBrokerById(String brokerId);
    Set<String> getBrokerIds();
}
