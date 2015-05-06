package com.amlinv.activemq.registry.impl;

import com.amlinv.activemq.registry.BrokerRegistry;
import com.amlinv.activemq.registry.model.BrokerInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by art on 5/2/15.
 */
public class DefaultBrokerRegistry implements BrokerRegistry {
    private final ConcurrentHashMap<String, BrokerInfo> registry = new ConcurrentHashMap<>();

    public DefaultBrokerRegistry() {
    }

    public DefaultBrokerRegistry(List<BrokerInfo> initBrokerList) {
        for ( BrokerInfo oneBrokerInfo : initBrokerList ) {
            this.registry.put(oneBrokerInfo.getBrokerId(), oneBrokerInfo);
        }
    }

    /**
     * Add the given broker to the registry, if another broker with the same ID does not already exist.
     *
     * @param newBrokerInfo detail of the broker to add to the registry.
     * @return true => if the broker is added; false => if another broker with the same ID already exists in the
     * registry and the new broker is therefore not added.
     */
    @Override
    public boolean addBroker(BrokerInfo newBrokerInfo) {
        boolean putInd;

        BrokerInfo prev;
        prev = registry.putIfAbsent(newBrokerInfo.getBrokerId(), newBrokerInfo);

        if (prev == null) {
            putInd = true;
        } else {
            putInd = false;
        }

        return putInd;
    }

    /**
     * Lookup the broker with the given ID in the registry.
     *
     * @param brokerId ID of the broker to lookup.
     * @return details for the broker with the given ID, if found; null otherwise.
     */
    @Override
    public BrokerInfo lookupBrokerById(String brokerId) {
        BrokerInfo result = this.registry.get(brokerId);

        return  result;
    }

    /**
     * Retrieve the set of all broker IDs in the registry.
     *
     * @return set of broker IDs.
     */
    @Override
    public Set<String> getBrokerIds() {
        return new HashSet<String>(this.registry.keySet());
    }
}
