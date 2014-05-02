package com.amlinv.activemq.bridge.engine;

import com.amlinv.util.event.SyncEventListenerUtil;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.command.ActiveMQMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;

/**
 * Abstract base class for Queue and Topic bridges which provides most of the generic functionality.  Current only
 * supports a single execution (i.e. it is only possible to start() once and stop() once).
 *
 * Created by art on 5/1/14.
 */
public abstract class AbstractDestinationBridge implements DestinationBridge {
    private static final Logger LOG = LoggerFactory.getLogger(QueueBridge.class);

    private static final String DEST_BRIDGE_MSG_NUM = "AmqBridgeMsgNum";

    /**
     * Cache the start and stop bridge event objects since they may get a fair amount of use and are light-weight to
     * keep around.
     */
    public static final DestinationBridgeEvent  DEST_BRIDGE_START_EVENT =
            new DestinationBridgeEvent(DestinationBridgeEventType.DESTINATION_BRIDGE_STARTED);
    public static final DestinationBridgeEvent  DEST_BRIDGE_STOP_EVENT =
            new DestinationBridgeEvent(DestinationBridgeEventType.DESTINATION_BRIDGE_STOPPED);
    public static final DestinationBridgeEvent  DEST_BRIDGE_MESSAGE_SENT_EVENT =
            new DestinationBridgeEvent(DestinationBridgeEventType.MESSAGE_SENT);
    public static final DestinationBridgeEvent  DEST_BRIDGE_MESSAGE_ERROR_EVENT =
            new DestinationBridgeEvent(DestinationBridgeEventType.MESSAGE_ERROR);
    public static final DestinationBridgeEvent  DEST_BRIDGE_MESSAGE_RECEVIED_EVENT =
            new DestinationBridgeEvent(DestinationBridgeEventType.MESSAGE_RECEIVED);

    /**
     * Connection to use for receiving messages from the source; do not close here.
     */
    private ActiveMQConnection srcConn;

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
    private String              destName;

    private SyncEventListenerUtil<DestinationBridgeEvent, DestinationBridgeListener> eventSendUtil;

    private boolean             started = false;
    private boolean             stopped = false;


    /**
     * Subclass responsibility which needs to create the consumer.  A message listener will be added to the returned
     * consumer.
     *
     * @param sess - the JMS session within which the consumer will operate; this is the same as the srcSesss and is
     *               only passed in as a convenience.
     * @return the consumer which will be used by the bridge to receive messages for forwarding.
     */
    protected abstract MessageConsumer  createConsumer (Session sess) throws JMSException;

    /**
     * Subclass responsibility which needs to create the producer.
     *
     * @param sess - the JMS session within which the producer will operate; this is the same as the destSesss and is
     *               only passed in as a convenience.
     * @return the producer which will be used to forward messages received by the bridge.
     */
    protected abstract MessageProducer  createProducer (Session sess) throws JMSException;

    public AbstractDestinationBridge (ActiveMQConnection inSrcConn, ActiveMQConnection inDestConn, String inDestName) {
        this.srcConn   = inSrcConn;
        this.destConn  = inDestConn;
        this.destName  = inDestName;
        this.eventSendUtil = new SyncEventListenerUtil<DestinationBridgeEvent, DestinationBridgeListener>();
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

            this.consumer = this.createConsumer(this.srcSess);
            this.producer = this.createProducer(this.destSess);

            this.consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    AbstractDestinationBridge.this.onSrcMessage(message);
                }
            });

            eventSendUtil.safeSyncSendEvent(DEST_BRIDGE_START_EVENT);
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

            eventSendUtil.safeSyncSendEvent(DEST_BRIDGE_STOP_EVENT);
            this.stopped = true;
        }

        closeSessionSafely(this.srcSess, "on shutdown");
        closeSessionSafely(this.destSess, "on shutdown");
    }

    /**
     * Add a listener for events on this destination bridge.
     *
     * @param listener
     */
    public void addListener (DestinationBridgeListener listener) {
        eventSendUtil.addListener(listener);
    }

    public void removeListener (DestinationBridgeListener listener) {
        eventSendUtil.removeListener(listener);
    }

    protected void  onSrcMessage (Message msg) {
        try {
            eventSendUtil.safeSyncSendEvent(DEST_BRIDGE_MESSAGE_RECEVIED_EVENT);
            if ( msg instanceof ActiveMQMessage) {
                ActiveMQMessage amqMsg = (ActiveMQMessage) msg;

                amqMsg.setReadOnlyProperties(false);
                amqMsg.setMarshalledProperties(null);
            }

            msg.setLongProperty(DEST_BRIDGE_MSG_NUM, this.numMsgPassed);
            this.producer.send(msg);
            eventSendUtil.safeSyncSendEvent(DEST_BRIDGE_MESSAGE_SENT_EVENT);

            this.srcSess.commit();

            this.numMsgPassed++;
        } catch ( JMSException jmsExc ) {
            eventSendUtil.safeSyncSendEvent(DEST_BRIDGE_MESSAGE_ERROR_EVENT);

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
