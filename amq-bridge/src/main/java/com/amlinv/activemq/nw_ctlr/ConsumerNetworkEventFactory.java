package com.amlinv.activemq.nw_ctlr;

import com.amlinv.activemq.nw_ctlr.event.AddConsumerEvent;
import com.amlinv.activemq.nw_ctlr.event.RemoveConsumerEvent;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.*;

import javax.jms.Destination;

/**
 * Created by art on 5/2/15.
 */
public class ConsumerNetworkEventFactory {
    public NetworkEvent createConsumerNetworkEvent(ConsumerInfo consumerInfo, String brokerId) {
        // TBD999: way to detect our own subscriptions, or will need a way later to at least count them

        boolean networkConsumer = consumerInfo.isNetworkSubscription();
        Destination dest = consumerInfo.getDestination();
        String consumerId = consumerInfo.getConsumerId().toString();

        return  new AddConsumerEvent(brokerId, dest, consumerId, networkConsumer);
    }

    public NetworkEvent createConsumerRemovalNetworkEvent (RemoveInfo removeInfo, String brokerId,
                                                           ActiveMQDestination advisoryDest) {
        NetworkEvent    result;

        if ( removeInfo.isConsumerRemove() ) {
            String advisoryDestName = advisoryDest.getPhysicalName();
            Destination dest = null;

            if ( advisoryDestName.startsWith(AdvisorySupport.QUEUE_CONSUMER_ADVISORY_TOPIC_PREFIX) ) {
                dest = new ActiveMQQueue(
                        advisoryDestName.substring(AdvisorySupport.QUEUE_CONSUMER_ADVISORY_TOPIC_PREFIX.length()));
            } else if ( advisoryDestName.startsWith(AdvisorySupport.TOPIC_CONSUMER_ADVISORY_TOPIC_PREFIX) ) {
                dest = new ActiveMQTopic(
                        advisoryDestName.substring(AdvisorySupport.TOPIC_CONSUMER_ADVISORY_TOPIC_PREFIX.length()));
            }

            result = new RemoveConsumerEvent(brokerId, dest, removeInfo.getObjectId().toString());
        } else {
            result = null;
        }

        return  result;
    }
}
