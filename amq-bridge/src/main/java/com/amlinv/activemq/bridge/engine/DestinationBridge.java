package com.amlinv.activemq.bridge.engine;

import com.amlinv.util.event.SyncEventListenerUtil;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;

/**
 * Bridge of a single Destination.
 *
 * Created by art on 4/23/14.
 */
public interface DestinationBridge {
    long getNumMsgPassed ();
    long getNumErrors ();

    void start () throws JMSException;
    void stop () throws JMSException;

    /**
     * Add a listener for events on this destination bridge.
     * 
     * @param listener
     */
    void addListener (DestinationBridgeListener listener);
    void removeListener (DestinationBridgeListener listener);
}
