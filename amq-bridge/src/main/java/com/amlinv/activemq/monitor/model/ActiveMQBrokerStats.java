package com.amlinv.activemq.monitor.model;

import com.amlinv.activemq.monitor.jmx.annotation.MBeanAttribute;
import com.amlinv.activemq.monitor.jmx.annotation.MBeanLocation;

/**
 * Created by art on 3/31/15.
 */
@MBeanLocation(onamePattern = "org.apache.activemq:type=Broker,brokerName=${brokerName}")
public class ActiveMQBrokerStats implements MBeanLocationParameterSource {
    private final String brokerName;

    private long averageMessageSize;

    private String uptime;

    private long memoryLimit;

    private int memoryPercentUsage;

    public ActiveMQBrokerStats(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public long getAverageMessageSize() {
        return averageMessageSize;
    }

    @MBeanAttribute(name = "AverageMessageSize", type = long.class)
    public void setAverageMessageSize(long averageMessageSize) {
        this.averageMessageSize = averageMessageSize;
    }

    public String getUptime() {
        return uptime;
    }

    @MBeanAttribute(name = "Uptime", type = String.class)
    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public long getMemoryLimit() {
        return memoryLimit;
    }

    @MBeanAttribute(name = "MemoryLimit", type = long.class)
    public void setMemoryLimit(long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public int getMemoryPercentUsage() {
        return memoryPercentUsage;
    }

    @MBeanAttribute(name = "MemoryPercentUsage", type = int.class)
    public void setMemoryPercentUsage(int memoryPercentUsage) {
        this.memoryPercentUsage = memoryPercentUsage;
    }

    @Override
    public String getParameter(String parameterName) {
        if ( parameterName.equals("brokerName") ) {
            return this.brokerName;
        }

        return  null;
    }
}
