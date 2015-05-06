package com.amlinv.activemq.nw_ctlr.event;

import com.amlinv.activemq.nw_ctlr.NetworkEvent;

import javax.jms.Destination;

/**
 * Created by art on 5/2/15.
 */
public class RemoveConsumerEvent implements NetworkEvent {
    private final String brokerId;
    private final Destination jmsDestination;
    private final String consumerId;

    public RemoveConsumerEvent(String brokerId, Destination jmsDestination, String consumerId) {
        this.brokerId = brokerId;
        this.jmsDestination = jmsDestination;
        this.consumerId = consumerId;
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
}
