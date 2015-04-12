package com.amlinv.activemq.monitor.model;

import com.amlinv.activemq.monitor.jmx.annotation.MBeanAttribute;
import com.amlinv.activemq.monitor.jmx.annotation.MBeanLocation;

/**
 * Created by art on 3/31/15.
 */
@MBeanLocation(onamePattern = "org.apache.activemq:type=Broker,brokerName=${brokerName},destinationType=Queue,destinationName=${queueName}")
public class ActiveMQQueueStats implements MBeanLocationParameterSource {
    private final String brokerName;
    private final String queueName;

    private long queueSize;
    private long enqueueCount;
    private long dequeueCount;
    private long numConsumers;
    private long numProducers;

    public ActiveMQQueueStats(String brokerName, String queueName) {
        this.brokerName = brokerName;
        this.queueName = queueName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public String getQueueName() {
        return queueName;
    }

    public long getQueueSize() {
        return queueSize;
    }

    @MBeanAttribute(name = "QueueSize", type = long.class)
    public void setQueueSize(long queueSize) {
        this.queueSize = queueSize;
    }

    public long getEnqueueCount() {
        return enqueueCount;
    }

    @MBeanAttribute(name = "EnqueueCount", type = long.class)
    public void setEnqueueCount(long enqueueCount) {
        this.enqueueCount = enqueueCount;
    }

    public long getDequeueCount() {
        return dequeueCount;
    }

    @MBeanAttribute(name = "DequeueCount", type = long.class)
    public void setDequeueCount(long dequeueCount) {
        this.dequeueCount = dequeueCount;
    }

    public long getNumConsumers() {
        return numConsumers;
    }

    @MBeanAttribute(name = "ConsumerCount", type = long.class)
    public void setNumConsumers(long numConsumers) {
        this.numConsumers = numConsumers;
    }

    public long getNumProducers() {
        return numProducers;
    }

    @MBeanAttribute(name = "ProducerCount", type = long.class)
    public void setNumProducers(long numProducers) {
        this.numProducers = numProducers;
    }

    @Override
    public String getParameter(String parameterName) {
        if ( parameterName.equals("brokerName") ) {
            return  this.brokerName;
        } else if ( parameterName.equals("queueName") ) {
            return  this.queueName;
        }

        return null;
    }

    /**
     * Return a new queue stats structure with the total of the stats from this structure and the one given.  Returning
     * a new structure keeps all three structures unchanged, in the manner of immutability, to make it easier to have
     * safe usage under concurrency.
     *
     * @param other
     * @param resultBrokerName
     * @return
     */
    public ActiveMQQueueStats add (ActiveMQQueueStats other, String resultBrokerName) {
        ActiveMQQueueStats result = new ActiveMQQueueStats(resultBrokerName, this.queueName);
        result.setDequeueCount(this.getDequeueCount() + other.getDequeueCount());
        result.setEnqueueCount(this.getEnqueueCount() + other.getEnqueueCount());
        result.setNumConsumers(this.getNumConsumers() + other.getNumConsumers());
        result.setNumProducers(this.getNumProducers() + other.getNumProducers());
        result.setQueueSize(this.getQueueSize() + other.getQueueSize());

        return  result;
    }

    /**
     * Return a copy of this queue stats structure.
     *
     *  @return
     */
    public ActiveMQQueueStats copy (String brokerName) {
        ActiveMQQueueStats result = new ActiveMQQueueStats(brokerName, this.queueName);
        result.setDequeueCount(this.getDequeueCount());
        result.setEnqueueCount(this.getEnqueueCount());
        result.setNumConsumers(this.getNumConsumers());
        result.setNumProducers(this.getNumProducers());
        result.setQueueSize(this.getQueueSize());

        return  result;
    }
}
