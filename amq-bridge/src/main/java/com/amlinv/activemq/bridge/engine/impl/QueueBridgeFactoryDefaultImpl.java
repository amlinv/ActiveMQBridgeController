package com.amlinv.activemq.bridge.engine.impl;

import com.amlinv.activemq.bridge.engine.QueueBridge;
import com.amlinv.activemq.bridge.engine.QueueBridgeFactory;
import org.apache.activemq.ActiveMQConnection;

/**
 * Default implementation of the QueueBridgeFactory which creates a new QueueBridge on each request.
 * Created by art on 4/23/14.
 */
public class QueueBridgeFactoryDefaultImpl implements QueueBridgeFactory {

    @Override
    public QueueBridge createBridge (ActiveMQConnection srcConn, ActiveMQConnection destConn, String queueName) {
        QueueBridge result;

        result = new QueueBridge(srcConn, destConn, queueName);

        return result;
    }
}
