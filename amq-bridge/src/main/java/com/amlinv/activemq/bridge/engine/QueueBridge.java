package com.amlinv.activemq.bridge.engine;

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
public class QueueBridge {
    private static final Logger LOG = LoggerFactory.getLogger(QueueBridge.class);
    private static final String QUEUE_BRIDGE_MSG_NUM = "AmqQueueBridgeMsgNum";

    /**
     * Connection to use for receiving messages from the source; do not close here.
     */
    private ActiveMQConnection  srcConn;

    /**
     * Connection to use for sending messages to the destination; do not close here.
     */
    private ActiveMQConnection  destConn;

    private Session             srcSess;
    private Session             destSess;
    private MessageConsumer     consumer;
    private MessageProducer     producer;
    private long                numMsgPassed;
    private long                numErrors;
    private String              queueName;

    private boolean             started = false;
    private boolean             stopped = false;

    public QueueBridge (ActiveMQConnection inSrcConn, ActiveMQConnection inDestConn, String qName) {
        this.srcConn   = inSrcConn;
        this.destConn  = inDestConn;
        this.queueName = qName;
    }

    public long getNumMsgPassed () {
        return  this.numMsgPassed;
    }

    public long getNumErrors () {
        return  this.numErrors;
    }

    public void start () throws JMSException {
        synchronized ( this ) {
            if ( this.started ) {
                throw new IllegalStateException("queue bridge already started");
            }
            this.started = true;
        }

        boolean complete = false;
        try {
            this.srcSess = this.srcConn.createSession(true, Session.AUTO_ACKNOWLEDGE);
            this.destSess = this.destConn.createSession(false, Session.AUTO_ACKNOWLEDGE);

            ActiveMQQueue amqQueue = new ActiveMQQueue(queueName);
            this.consumer = this.srcSess.createConsumer(amqQueue);
            this.producer = this.destSess.createProducer(amqQueue);

            this.consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    QueueBridge.this.onSrcMessage(message);
                }
            });
            complete = true;
        } finally {
            if ( ! complete ) {
                this.closeSessionSafely(this.srcSess, "on failed startup of queue bridge");
                this.closeSessionSafely(this.destSess, "on failed startup of queue bridge");
            }
        }
    }

    public void stop () throws JMSException {
        synchronized ( this ) {
            if ( ! this.started ) {
                throw new IllegalStateException("attempt to stop when not running");
            } else if ( this.stopped ) {
                throw new IllegalStateException("attempt to stop when already stopped (or stopping)");
            }

            this.stopped = true;
        }

        closeSessionSafely(this.srcSess, "on shutdown");
        closeSessionSafely(this.destSess, "on shutdown");
    }

    protected void  onSrcMessage (Message msg) {
        try {
            if ( msg instanceof ActiveMQMessage ) {
                ActiveMQMessage amqMsg = (ActiveMQMessage) msg;

                amqMsg.setReadOnlyProperties(false);
                amqMsg.setMarshalledProperties(null);
            }

            msg.setLongProperty(QUEUE_BRIDGE_MSG_NUM, this.numMsgPassed);
            this.producer.send(msg);

            this.srcSess.commit();

            this.numMsgPassed++;
        } catch ( JMSException jmsExc ) {
            LOG.error("failed to forward message num {}", jmsExc);
            LOG.debug("failed message = {}", msg);

            this.numErrors++;

            try {
                this.srcSess.rollback();
            } catch ( JMSException rollbackExc ) {
                LOG.info("error on rollback after forwarding failure", rollbackExc);
            }
        }
    }

    protected void  closeSessionSafely (Session sess, String msg) {
        if ( sess == null )
            return;

        try {
            sess.close();
        } catch ( Throwable thrown ) {
            LOG.warn("error on closing session {}", thrown);
        }
    }
}
