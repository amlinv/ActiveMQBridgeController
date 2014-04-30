package com.amlinv.activemq.bridge.ctlr.events;

import com.amlinv.util.event.EventListener;

/**
 * Listener for events on a bridge.
 *
 * Created by art on 4/23/14.
 */
public interface AmqBridgeListener extends EventListener<AmqBridgeEvent> {
    void onEvent(AmqBridgeEvent event);
}
