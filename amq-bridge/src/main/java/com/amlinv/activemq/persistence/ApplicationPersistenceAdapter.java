package com.amlinv.activemq.persistence;

import com.amlinv.activemq.registry.BrokerRegistry;
import com.amlinv.activemq.registry.DestinationRegistry;

import java.io.IOException;
import java.util.Set;

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
