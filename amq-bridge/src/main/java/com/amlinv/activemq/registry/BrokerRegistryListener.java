package com.amlinv.activemq.registry;

import com.amlinv.activemq.registry.model.BrokerInfo;
import com.amlinv.server.util.ConcurrentRegistry;
import com.amlinv.server.util.RegistryListener;

/**
 * Created by art on 5/16/15.
 */
public interface BrokerRegistryListener extends RegistryListener<String, BrokerInfo> {
}
