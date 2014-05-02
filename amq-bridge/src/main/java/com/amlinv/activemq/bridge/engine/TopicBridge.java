package com.amlinv.activemq.bridge.engine;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Bridge of a single Queue.  Current only supports a single execution (i.e. it is only possible to start() once and
 * stop() once).
 *
 * Created by art on 4/23/14.
 */
public class TopicBridge extends AbstractDestinationBridge {
    private static final Logger LOG = LoggerFactory.getLogger(TopicBridge.class);

    private final ActiveMQTopic topic;

    public TopicBridge(ActiveMQConnection inSrcConn, ActiveMQConnection inDestConn, String qName) {
        super(inSrcConn, inDestConn, qName);
        this.topic = new ActiveMQTopic(qName);
    }

    protected MessageConsumer   createConsumer (Session sess) throws JMSException {
        MessageConsumer result = sess.createConsumer(this.topic);
        return  result;
    }

    protected MessageProducer   createProducer (Session sess) throws JMSException {
        MessageProducer result = sess.createProducer(this.topic);
        return  result;
    }
}
