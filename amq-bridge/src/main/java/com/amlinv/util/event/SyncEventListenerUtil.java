package com.amlinv.util.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for managing a list of listeners and asynchronously queueing event messages to send to those
 * listeners.
 *
 * Created by art on 4/26/14.
 */
public class SyncEventListenerUtil<EVENT_TYPE, EVENT_LISTENER extends EventListener<EVENT_TYPE>> {
    private static final Logger         LOG = LoggerFactory.getLogger(SyncEventListenerUtil.class);

    private final List<EVENT_LISTENER>  listeners = new LinkedList<EVENT_LISTENER>();

    public SyncEventListenerUtil() {
    }

    public void addListener (EVENT_LISTENER listener) {
        synchronized ( this.listeners ) {
            this.listeners.add(listener);
        }
    }

    public boolean  removeListener (EVENT_LISTENER listener) {
        boolean         rmInd;

        synchronized ( this.listeners ) {
            rmInd = this.listeners.remove(listener);
        }

        return  rmInd;
    }

    public void syncSendEvent (EVENT_TYPE event) {
        doSendEvent(event, false);
    }

    public void safeSyncSendEvent (EVENT_TYPE event) {
        doSendEvent(event, true);
    }

    public void shutdown () {
    }

    protected void  doSendEvent (EVENT_TYPE event, boolean safeInd) {
        List<EVENT_LISTENER>    curListeners;
        synchronized ( this.listeners ) {
            curListeners = new LinkedList<EVENT_LISTENER>(this.listeners);
        }

        for ( EVENT_LISTENER oneListener : curListeners ) {
            if ( safeInd ) {
                try {
                    oneListener.onEvent(event);
                } catch ( Throwable thrown ) {
                    LOG.warn("error on sending synchronous event to listener", thrown);
                }
            } else {
                oneListener.onEvent(event);
            }
        }
    }
}
