package com.amlinv.activemq.bridge.engine;

import com.amlinv.activemq.bridge.engine.impl.QueueBridgeFactoryDefaultImpl;
import com.amlinv.activemq.bridge.model.AmqBridgeSpec;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Bridge which moves messages between two ActiveMQ instances.
 *
 * Created by art on 4/23/14.
 */
public class AmqBridge {
    private static final Logger LOG = LoggerFactory.getLogger(AmqBridge.class);

    private ActiveMQConnection          srcConn;
    private ActiveMQConnection          destConn;
    private AmqBridgeSpec               spec;
    private boolean                     runningInd = false;
    private boolean                     stoppingInd = false;
    private List<String>                queueList;
    private Map<String, QueueBridge>    queueBridges = new HashMap<String, QueueBridge>();
    private QueueBridgeFactory          queueBridgeFactory = new QueueBridgeFactoryDefaultImpl();

    public void setAmqBridgeSpec (AmqBridgeSpec inSpec) {
        this.spec = inSpec;
        this.queueList = this.spec.getQueueList();
    }

    /**
     * Add a queue to the bridge and start forwarding messages from the queue at the source URL to the same queue at
     * the destination URL.
     *
     * @param queueName - name of the Queue to forward.
     * @throws JMSException - on error from ActiveMQ.
     */
    public void addQueue (String queueName) throws JMSException {
        if ( this.queueList == null ) {
            this.queueList = new LinkedList<String>();

        }

        this.queueList.add(queueName);

        boolean startQueue = false;
        synchronized ( this ) {
            // Don't bother trying to start the queue if the bridge is not running, or is shutting down.
            if ( ( this.runningInd ) && ( ! this.stoppingInd ) ) {
                startQueue = true;
            }
        }

        if ( startQueue ) {
            this.startQueueBridge(queueName);
        }
    }

    public void start () throws JMSException, AmqBridgeConfigurationException {
        synchronized ( this ) {
            if ( this.stoppingInd ) {
                throw new IllegalStateException("bridge is in the process of shutting down");
            } else if ( this.runningInd ) {
                throw new IllegalStateException("bridge is already running");
            }

            this.runningInd = true;
        }

        if ( spec == null ) {
            throw new AmqBridgeConfigurationException("bridge specification is null");
        }
        if ( spec.getSrcUrl() == null ) {
            throw new AmqBridgeConfigurationException("bridge source URL must not be null");
        }
        if ( spec.getDestUrl() == null ) {
            throw new AmqBridgeConfigurationException("bridge destination URL must not be null");
        }

        //
        // TBD: make this asynchronous - either here, or above here - as the connection start() can block.
        //
        this.srcConn = null;
        this.destConn = null;
        boolean complete = false;
        try {
            this.srcConn = createConnection(spec.getSrcUrl());
            this.srcConn.start();
            this.destConn = createConnection(spec.getDestUrl());
            this.destConn.start();
            // TBD: add exception handlers (and transport listeners)

            this.startConsumers();

            complete = true;
        } finally {
            if ( ! complete ) {
                closeSafely(this.srcConn, "on failed startup of bridge");
                this.srcConn = null;

                closeSafely(this.destConn, "on failed startup of bridge");
                this.srcConn = null;
            }
        }
    }

    public void stop () throws JMSException, AmqBridgeConfigurationException {
        // TBD: use ServiceController
        synchronized ( this ) {
            if (stoppingInd) {
                throw new IllegalStateException("bridge is already in the process of shutting down");
            } else if (!this.runningInd) {
                throw new IllegalStateException("bridge is not running");
            }

            this.stoppingInd = true;
        }

        closeSafely(this.srcConn, "on failed startup of bridge");
        this.srcConn = null;

        closeSafely(this.destConn, "on failed startup of bridge");
        this.srcConn = null;
    }

    protected ActiveMQConnection    createConnection (String url) throws JMSException {
        ActiveMQConnectionFactory   connFactory = new ActiveMQConnectionFactory(url);

        return  (ActiveMQConnection) connFactory.createConnection();
    }

    protected void  closeSafely (ActiveMQConnection conn, String msg) {
        if ( conn == null )
            return;

        try {
            //
            // If you find a stack trace for an exception coming from here even though the catch block was not executed,
            //  that's ActiveMQ internal magic which remembers the close() here and reports it as the cause of an
            //  exception somewhere else.  Ignore this part of the stack trace.
            //
            conn.close();
        } catch ( Throwable thrown ) {
            LOG.warn("error on closing ActiveMQ connection {}", msg, thrown);
        }
    }

    protected void startConsumers() throws JMSException {
        for ( String queueName : this.queueList ) {
            this.startQueueBridge(queueName);
        }
    }

    protected void  startQueueBridge (String queueName) throws JMSException {
        QueueBridge newBridge = queueBridgeFactory.createBridge(this.srcConn, this.destConn, queueName);

        synchronized ( this.queueBridges ) {
            if ( this.queueBridges.containsKey(queueName) ) {
                LOG.debug("skipping duplicate attempt to start queue bridge for queue {}", queueName);
                return;
            }

            this.queueBridges.put(queueName, newBridge);
        }

        newBridge.start();
    }
}
