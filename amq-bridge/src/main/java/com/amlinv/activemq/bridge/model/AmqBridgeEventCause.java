package com.amlinv.activemq.bridge.model;

/**
 * Describe the cause of an event, such as a user operation requested via UI.
 *
 * Created by art on 4/23/14.
 */
public class AmqBridgeEventCause {
    public String               reason;
    public AmqBridgeEventSource source;
}
