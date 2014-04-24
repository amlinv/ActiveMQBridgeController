package com.amlinv.activemq.bridge.engine;

/**
 * Exception indicating an improperly configured ActiveMQ bridge.
 *
 * Created by art on 4/23/14.
 */
public class AmqBridgeConfigurationException extends Exception {
    public AmqBridgeConfigurationException (String reason) {
        super(reason);
    }
}
