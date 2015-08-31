package com.amlinv.activemq.persistence;

import com.amlinv.activemq.topo.registry.BrokerRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistry;

import java.io.IOException;

/**
 * Created by art on 5/16/15.
 */
public interface ApplicationPersistenceAdapter {
    void setQueueRegistry(DestinationRegistry queueRegistry);
    void setTopicRegistry(DestinationRegistry topicRegistry);
    void setBrokerRegistry(BrokerRegistry brokerRegistry);
    void load() throws IOException;
    void save() throws IOException;
}
