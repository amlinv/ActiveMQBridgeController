package com.amlinv.activemq.bridge.ctlr.events;

/**
 * Created by art on 4/23/14.
 */
public class AmqBridgeEvent {
    private AmqBridgeEventCause cause;
    private AmqBridgeEventType  type;
    private Object              data;

    public AmqBridgeEventCause getCause() {
        return cause;
    }

    public void setCause(AmqBridgeEventCause cause) {
        this.cause = cause;
    }

    public AmqBridgeEventType getType() {
        return type;
    }

    public void setType(AmqBridgeEventType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
