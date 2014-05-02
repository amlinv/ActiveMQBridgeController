package com.amlinv.activemq.bridge.engine;

import com.amlinv.util.event.EventListenerAsyncUtil;
import com.amlinv.util.event.SyncEventListenerUtil;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQMessageAudit;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;

/**
 * Bridge of a single Queue.  Current only supports a single execution (i.e. it is only possible to start() once and
 * stop() once).
 *
 * Created by art on 4/23/14.
 */
public class QueueBridge extends AbstractDestinationBridge {
    private static final Logger LOG = LoggerFactory.getLogger(QueueBridge.class);

    private final ActiveMQQueue queue;

    public QueueBridge (ActiveMQConnection inSrcConn, ActiveMQConnection inDestConn, String qName) {
        super(inSrcConn, inDestConn, qName);
        this.queue = new ActiveMQQueue(qName);
    }

    protected MessageConsumer   createConsumer (Session sess) throws JMSException {
        MessageConsumer result = sess.createConsumer(this.queue);
        return  result;
    }

    protected MessageProducer   createProducer (Session sess) throws JMSException {
        MessageProducer result = sess.createProducer(this.queue);
        return  result;
    }
}
