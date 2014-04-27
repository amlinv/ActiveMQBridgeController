package com.amlinv.activemq.bridge.ctlr.events;

import com.amlinv.util.thread.DaemonThreadFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor which queues up events to send to AmqBridgeListener instances and sends them using a thread pool.
 *
 * Created by art on 4/23/14.
 */
public class AmqBridgeEventSendExecutor {
    private ThreadPoolExecutor  executor = new ThreadPoolExecutor(3, 5, 3, TimeUnit.SECONDS,
                                                                  new ArrayBlockingQueue<Runnable>(25),
                                                                  new DaemonThreadFactory("amq-bridge-event-sender-"));

    public AmqBridgeEventSendExecutor () {
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void queueEventSend (final AmqBridgeEvent event, List<AmqBridgeListener> listeners) {
        for ( final AmqBridgeListener oneListener : listeners ) {
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
