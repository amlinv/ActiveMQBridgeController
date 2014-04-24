package com.amlinv.activemq.bridge.ctlr;

/**
 * Exception thrown on attempt to stop a bridge that is not active.
 *
 * Created by art on 4/23/14.
 */
public class BridgeNotActiveException extends Exception {
    public BridgeNotActiveException (String id) {
        super("bridge \"" + id + "\" is not active");
    }
}
