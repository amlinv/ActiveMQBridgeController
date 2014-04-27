package com.amlinv.activemq.bridge.ctlr.events;

import com.amlinv.activemq.bridge.engine.AmqBridge;

/**
 * Describe the cause of an event, such as a user operation requested via UI.
 *
 * Created by art on 4/23/14.
 */
public class AmqBridgeEventCause {
    public String               reason;
    public AmqBridgeEventSource source;

    public AmqBridgeEventCause (String inReason, AmqBridgeEventSource inSource) {
        this.reason = inReason;
        this.source = inSource;
    }
}
