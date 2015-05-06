package com.amlinv.activemq.registry;

import com.amlinv.activemq.registry.model.DestinationInfo;

/**
 * Created by art on 5/2/15.
 */
public interface DestinationRegistry {
    boolean addDestination(DestinationInfo info);
    DestinationInfo lookupDestination(String name);
}
