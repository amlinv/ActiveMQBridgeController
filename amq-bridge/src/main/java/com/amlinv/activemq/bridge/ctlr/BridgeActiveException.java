package com.amlinv.activemq.bridge.ctlr;

/**
 *
 * Created by art on 4/26/14.
 */
public class BridgeActiveException extends Exception {
    public BridgeActiveException(String id) {
        super("bridge \"" + id + "\" is active; please stop first");
    }
}
