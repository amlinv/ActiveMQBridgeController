package com.amlinv.activemq.bridge.ctlr;

/**
 * Exception thrown when a operation which requires a running bridge controller is requested on a bridge controller
 * that is not running.
 *
 * Created by art on 4/23/14.
 */
public class BridgeControllerNotActiveException extends Exception {
}
