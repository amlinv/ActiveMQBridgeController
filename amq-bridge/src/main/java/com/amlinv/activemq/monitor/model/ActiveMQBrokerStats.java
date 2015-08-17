package com.amlinv.activemq.monitor.model;

import com.amlinv.jmxutil.MBeanLocationParameterSource;
import com.amlinv.jmxutil.annotation.MBeanAttribute;
import com.amlinv.jmxutil.annotation.MBeanLocation;

/**
 * Created by art on 3/31/15.
 */
@MBeanLocation(onamePattern = "org.apache.activemq:type=Broker,brokerName=${brokerName}")
public class ActiveMQBrokerStats implements MBeanLocationParameterSource {
    private final String brokerName;

    private long averageMessageSize;
    private String uptime;
    private long uptimeMillis;
    private long memoryLimit;
    private long memoryPercentUsage;
    private long storePercentUsage;
    private long currentConnectionsCount;
    private long totalConsumerCount;
    private long totalMessageCount;
    private long totalEnqueueCount;
    private long totalDequeueCount;

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

    public long getUptimeMillis() {
        return uptimeMillis;
    }

    @MBeanAttribute(name = "UptimeMillis", type = long.class)
    public void setUptimeMillis(long uptimeMillis) {
        this.uptimeMillis = uptimeMillis;
    }

    public long getMemoryLimit() {
        return memoryLimit;
    }

    @MBeanAttribute(name = "MemoryLimit", type = long.class)
    public void setMemoryLimit(long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public long getMemoryPercentUsage() {
        return memoryPercentUsage;
    }

    @MBeanAttribute(name = "MemoryPercentUsage", type = int.class)
    public void setMemoryPercentUsage(long memoryPercentUsage) {
        this.memoryPercentUsage = memoryPercentUsage;
    }

    public long getCurrentConnectionsCount() {
        return currentConnectionsCount;
    }

    public long getStorePercentUsage() {
        return storePercentUsage;
    }

    @MBeanAttribute(name = "StorePercentUsage", type = int.class)
    public void setStorePercentUsage(long storePercentUsage) {
        this.storePercentUsage = storePercentUsage;
    }

    @MBeanAttribute(name = "CurrentConnectionsCount", type = long.class)
    public void setCurrentConnectionsCount(long currentConnectionsCount) {
        this.currentConnectionsCount = currentConnectionsCount;
    }

    public long getTotalConsumerCount() {
        return totalConsumerCount;
    }

    @MBeanAttribute(name = "TotalConsumerCount", type = long.class)
    public void setTotalConsumerCount(long totalConsumerCount) {
        this.totalConsumerCount = totalConsumerCount;
    }

    public long getTotalMessageCount() {
        return totalMessageCount;
    }

    @MBeanAttribute(name = "TotalMessageCount", type = long.class)
    public void setTotalMessageCount(long totalMessageCount) {
        this.totalMessageCount = totalMessageCount;
    }

    public long getTotalEnqueueCount() {
        return totalEnqueueCount;
    }

    @MBeanAttribute(name = "TotalEnqueueCount", type = long.class)
    public void setTotalEnqueueCount(long totalEnqueueCount) {
        this.totalEnqueueCount = totalEnqueueCount;
    }

    public long getTotalDequeueCount() {
        return totalDequeueCount;
    }

    @MBeanAttribute(name = "TotalDequeueCount", type = long.class)
    public void setTotalDequeueCount(long totalDequeueCount) {
        this.totalDequeueCount = totalDequeueCount;
    }

    @Override
    public String getParameter(String parameterName) {
        if ( parameterName.equals("brokerName") ) {
            return this.brokerName;
        }

        return  null;
    }
}
