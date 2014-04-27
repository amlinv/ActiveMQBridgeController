package com.amlinv.activemq.bridge.ctlr.events;

/**
 * Listener for events on a bridge.
 *
 * Created by art on 4/23/14.
 */
public interface AmqBridgeListener {
    void onEvent(AmqBridgeEvent event);
}
