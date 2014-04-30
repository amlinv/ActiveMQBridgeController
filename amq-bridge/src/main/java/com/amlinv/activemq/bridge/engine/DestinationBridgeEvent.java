package com.amlinv.activemq.bridge.engine;

/**
 * Created by art on 4/28/14.
 */
public class DestinationBridgeEvent {
    private DestinationBridgeEventType  type;

    public DestinationBridgeEvent(DestinationBridgeEventType newType) {
        this.type = newType;
    }
    public DestinationBridgeEventType   getType() {
        return type;
    }

    public void setType (DestinationBridgeEventType newType) {
        this.type = newType;
    }
}
