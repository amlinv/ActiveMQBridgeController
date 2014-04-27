package com.amlinv.util.event;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for managing a list of listeners and asynchronously queueing event messages to send to those
 * listeners.
 *
 * Created by art on 4/26/14.
 */
public class EventListenerAsyncUtil<EVENT_TYPE, EVENT_LISTENER extends EventListener<EVENT_TYPE>> {
    private final List<EVENT_LISTENER>  listeners = new LinkedList<EVENT_LISTENER>();
    private final EventSendExecutor<EVENT_TYPE, EVENT_LISTENER> executor;

    public EventListenerAsyncUtil(String threadNamingPrefix, int coreSize, int maxThread, int timeout, int maxQueued) {
        this.executor = new EventSendExecutor<EVENT_TYPE, EVENT_LISTENER>(threadNamingPrefix, coreSize, maxThread,
                                                                          timeout, maxQueued);
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

    public void queueEventSend (EVENT_TYPE event) {
        List<EVENT_LISTENER>    curListeners;
        synchronized ( this.listeners ) {
            curListeners = new LinkedList<EVENT_LISTENER>(this.listeners);
        }

        this.executor.queueEventSend(event, curListeners);
    }
}
