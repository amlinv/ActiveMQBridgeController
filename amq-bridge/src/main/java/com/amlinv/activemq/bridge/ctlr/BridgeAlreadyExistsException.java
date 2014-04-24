package com.amlinv.activemq.bridge.ctlr;

/**
 * Indicates an attempt to create a bridge that already exists.
 *
 * Created by art on 4/23/14.
 */
public class BridgeAlreadyExistsException extends Exception {
    public BridgeAlreadyExistsException(String id) {
        super("bridge \"" + id + "\" already exists");
    }
}
