package com.amlinv.activemq.nw_ctlr.event;

import com.amlinv.activemq.nw_ctlr.NetworkEvent;

import javax.jms.Destination;

/**
 * Created by art on 5/2/15.
 */
public class AddConsumerEvent implements NetworkEvent {
    private final String brokerId;
    private final Destination jmsDestination;
    private final String consumerId;
    private final boolean networkConsumer;

    public AddConsumerEvent(String brokerId, Destination jmsDestination, String consumerId, boolean networkConsumer) {
        this.brokerId = brokerId;
        this.jmsDestination = jmsDestination;
        this.consumerId = consumerId;
        this.networkConsumer = networkConsumer;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public Destination getJmsDestination() {
        return jmsDestination;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public boolean isNetworkConsumer() {
        return networkConsumer;
    }
}
