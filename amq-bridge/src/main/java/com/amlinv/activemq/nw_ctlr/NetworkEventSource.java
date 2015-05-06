package com.amlinv.activemq.nw_ctlr;

/**
 * Source of events used by a network controller in making decisions.
 *
 * Created by art on 5/2/15.
 */
public interface NetworkEventSource {
    /**
     * Set the listener for events to the one given.  The source is responsible for ensuring race conditions do not
     * exist between the creation of the source and configuration of the listener.
     *
     * @param listener
     */
    void setListener(NetworkEventListener listener);
}
