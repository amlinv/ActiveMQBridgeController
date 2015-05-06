package com.amlinv.activemq.registry.model;

/**
 * Created by art on 5/2/15.
 */
public class BrokerInfo {
    private final String brokerId;
    private final String brokerName;
    private final String brokerUrl;

    public BrokerInfo(String brokerId, String brokerName, String brokerUrl) {
        this.brokerId = brokerId;
        this.brokerName = brokerName;
        this.brokerUrl = brokerUrl;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }
}
