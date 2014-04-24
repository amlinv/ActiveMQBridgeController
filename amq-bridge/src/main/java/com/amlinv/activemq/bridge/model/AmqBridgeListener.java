package com.amlinv.activemq.bridge.model;

/**
 * Listener for events on a bridge.
 *
 * Created by art on 4/23/14.
 */
public interface AmqBridgeListener {
    void    bridgeStarted(AmqBridgeEventCause cause);
    void    bridgeStopped(AmqBridgeEventCause cause);
}
