package com.amlinv.activemq.nw_ctlr;

/**
 * Created by art on 5/2/15.
 */
public interface NetworkController {
    void start();
    void stop();

    void addNetworkEventSource(NetworkEventSource newEventSource);
}