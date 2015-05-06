package com.amlinv.activemq.registry.impl;

import com.amlinv.activemq.registry.DestinationRegistry;
import com.amlinv.activemq.registry.model.DestinationInfo;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by art on 5/2/15.
 */
public class DefaultDestinationRegistry implements DestinationRegistry {

    private final ConcurrentHashMap<String, DestinationInfo> registry = new ConcurrentHashMap<>();

    @Override
    public boolean addDestination(DestinationInfo info) {
        DestinationInfo prev = this.registry.putIfAbsent(info.getName(), info);

        boolean putInd;
        if ( prev == null ) {
            putInd = true;
        } else {
            putInd = false;
        }

        return putInd;
    }

    @Override
    public DestinationInfo lookupDestination(String name) {
        DestinationInfo result = this.registry.get(name);

        return result;
    }
}
