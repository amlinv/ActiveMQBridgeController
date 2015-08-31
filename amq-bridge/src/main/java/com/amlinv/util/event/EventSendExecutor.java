package com.amlinv.util.event;

import com.amlinv.thread.util.DaemonThreadFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * NOTE: can lose events when the executor runs out of resources.
 *
 * Created by art on 4/26/14.
 */
public class EventSendExecutor<EVENT_TYPE, LISTENER_TYPE extends EventListener<EVENT_TYPE>> {
    private final ThreadPoolExecutor    executor;

    public EventSendExecutor (String threadNamingPrefix, int coreSize, int maxThread, int timeout, int maxQueued) {
        this.executor = new ThreadPoolExecutor(coreSize, maxThread, timeout, TimeUnit.MILLISECONDS,
                                               new ArrayBlockingQueue<Runnable>(maxQueued),
                                               new DaemonThreadFactory(threadNamingPrefix));

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void queueEventSend (final EVENT_TYPE event, List<LISTENER_TYPE> listeners) {
        for ( final LISTENER_TYPE oneListener : listeners ) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    oneListener.onEvent(event);
                }
            });
        }
    }

    public void shutdown () {
        executor.shutdown();
    }

}
