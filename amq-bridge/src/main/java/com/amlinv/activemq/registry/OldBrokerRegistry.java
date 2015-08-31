package com.amlinv.activemq.registry;

import com.amlinv.activemq.topo.registry.model.BrokerInfo;

import java.util.Set;

/**
 * Created by art on 5/2/15.
 */
@Deprecated
public interface OldBrokerRegistry {
    boolean addBroker(BrokerInfo newBrokerInfo);
    BrokerInfo lookupBrokerById(String brokerId);
    Set<String> getBrokerIds();
}
