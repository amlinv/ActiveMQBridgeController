package com.amlinv.activemq.nw_ctlr.impl;

import com.amlinv.activemq.nw_ctlr.ConsumerNetworkEventFactory;
import com.amlinv.activemq.nw_ctlr.NetworkEvent;
import com.amlinv.activemq.nw_ctlr.NetworkEventListener;
import com.amlinv.activemq.nw_ctlr.NetworkEventSource;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.RemoveInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TBD: add reconnect logic, including use of ExceptionListener on the connection
 *
 * Created by art on 5/2/15.
 */
public class ActiveMQConsumerAdvisoryEventSource implements NetworkEventSource {
    private static final Logger DefaultLogger = LoggerFactory.getLogger(ActiveMQConsumerAdvisoryEventSource.class);

    private Logger log = DefaultLogger;

    private final ConnectionFactory connectionFactory;
    private final String brokerId;
    private final Executor executor;

    private Connection connection;

    private NetworkEventListener listener;
    private ConsumerNetworkEventFactory consumerNetworkEventFactory = new ConsumerNetworkEventFactory();
    private AtomicBoolean started = new AtomicBoolean(false);
    private ConsumerAdvisoryListener myJmsListener = new ConsumerAdvisoryListener();

    public ActiveMQConsumerAdvisoryEventSource(ConnectionFactory connectionFactory, String initBrokerId,
                                               Executor initExecutor) {

        this.connectionFactory = connectionFactory;
        this.brokerId = initBrokerId;

        if ( initExecutor == null ) {
            this.executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
        } else {
            this.executor = initExecutor;
        }
    }

    @Override
    public void setListener(NetworkEventListener newListener) {
        this.listener = newListener;

        if ( started.compareAndSet(false, true) ) {
            this.start();
        }
    }

    protected void  start () {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                createConnection();
            }
        });
    }

    protected void createConnection() {
        try {
            this.connection = connectionFactory.createConnection();

            Session sess = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination dest = sess.createTopic(AdvisorySupport.CONSUMER_ADVISORY_TOPIC_PREFIX + ">");

            MessageConsumer consumer = sess.createConsumer(dest);
            consumer.setMessageListener(this.myJmsListener);

            this.connection.start();
        } catch ( JMSException jmsExc ) {
            this.log.error("failed to connect to broker", jmsExc);
            this.safeCloseConnection();
        }
    }

    protected void safeCloseConnection() {
        Connection closeConn;
        synchronized ( this ) {
            closeConn = this.connection;
            this.connection = null;
        }

        if ( closeConn != null ) {
            try {
                closeConn.close();
            } catch ( JMSException jmsExc ) {
                this.log.warn("failed to close JMS connection", jmsExc);
            }
        }
    }

    protected void onConsumerAdvisoryMessage (Message message) {
        // TBD: need a way to distinguish true "end" consumers versus bridges - preferrably both AMQ bridges and those
        // TBD: created by this, or another, network controller
        if ( message instanceof ActiveMQMessage) {
            ActiveMQMessage amqMessage = (ActiveMQMessage) message;

            DataStructure dataStructure = amqMessage.getDataStructure();
            if ( dataStructure instanceof ConsumerInfo) {
                ConsumerInfo consumerInfo = (ConsumerInfo) dataStructure;

                NetworkEvent event = this.consumerNetworkEventFactory.createConsumerNetworkEvent(consumerInfo,
                        this.brokerId);

                this.listener.onNetworkEvent(event);
            } else if ( dataStructure instanceof RemoveInfo) {
                RemoveInfo removeInfo = (RemoveInfo) dataStructure;
                NetworkEvent event = this.consumerNetworkEventFactory.createConsumerRemovalNetworkEvent(removeInfo,
                        this.brokerId, amqMessage.getDestination());

                if ( event != null ) {
                    this.listener.onNetworkEvent(event);
                } else {
                    this.log.warn("received removal event on consumer advisory for removal of non-consumer: " +
                            "removal-type={}; message-id={}", removeInfo.getClass().getName(),
                            amqMessage.getJMSMessageID());
                }
            } else {
                if ( dataStructure == null ) {
                    this.log.warn("received activemq-message with null data: message-id={}", amqMessage.getJMSMessageID());
                } else {
                    this.log.warn("received activemq-message with non-consumer-info data: data-type={}; message-id={}",
                            dataStructure.getClass().getName(), amqMessage.getJMSMessageID());
                }
            }
        } else {
            this.log.warn("received non-activemq-message on consumer advisory topic: message-type={}",
                    message.getClass().getName());
        }
    }

    protected class ConsumerAdvisoryListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            onConsumerAdvisoryMessage(message);
        }
    }
}
