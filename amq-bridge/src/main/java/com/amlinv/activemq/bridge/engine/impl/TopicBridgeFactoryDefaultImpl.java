package com.amlinv.activemq.bridge.engine.impl;

import com.amlinv.activemq.bridge.engine.TopicBridge;
import com.amlinv.activemq.bridge.engine.TopicBridgeFactory;
import org.apache.activemq.ActiveMQConnection;

/**
 * Default implementation of the QueueBridgeFactory which creates a new QueueBridge on each request.
 * Created by art on 4/23/14.
 */
public class TopicBridgeFactoryDefaultImpl implements TopicBridgeFactory {

    @Override
    public TopicBridge createBridge (ActiveMQConnection srcConn, ActiveMQConnection destConn, String queueName) {
        TopicBridge result;

        result = new TopicBridge(srcConn, destConn, queueName);

        return result;
    }
}
