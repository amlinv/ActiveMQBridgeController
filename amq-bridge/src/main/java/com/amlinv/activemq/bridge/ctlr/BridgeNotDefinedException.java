package com.amlinv.activemq.bridge.ctlr;

/**
 * Exception thrown on attempts to control a bridge with an ID that does not exist.
 *
 * Created by art on 4/23/14.
 */
public class BridgeNotDefinedException extends Exception {
    public BridgeNotDefinedException (String id) {
        super("bridge \"" + id + "\" not defined");
    }
}
