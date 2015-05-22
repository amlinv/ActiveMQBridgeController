package com.amlinv.activemq.registry;

import com.amlinv.activemq.registry.model.DestinationInfo;
import com.amlinv.activemq.registry.model.DestinationState;
import com.amlinv.server.util.RegistryListener;

/**
 * Listener to registry events on a DestinationRegistry.
 *
 * Created by art on 5/2/15.
 */
public interface DestinationRegistryListener extends RegistryListener<String, DestinationState> {
}
