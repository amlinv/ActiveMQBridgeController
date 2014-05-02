package com.amlinv.activemq.bridge.engine;

import org.apache.activemq.ActiveMQConnection;

/**
 * Factory for creating Queue bridges.
 *
 * Created by art on 4/23/14.
 */
public interface TopicBridgeFactory {
    TopicBridge createBridge(ActiveMQConnection srcConn, ActiveMQConnection destConn, String queueName);
}
