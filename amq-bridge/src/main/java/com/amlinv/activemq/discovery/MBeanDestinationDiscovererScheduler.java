package com.amlinv.activemq.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by art on 5/20/15.
 */
public class MBeanDestinationDiscovererScheduler {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MBeanDestinationDiscovererScheduler.class);

    private Logger log = DEFAULT_LOGGER;

    private ScheduledExecutorService executor;
    private MBeanDestinationDiscoverer discoverer;
    private long interPollDelay = 5000;

    private ScheduledFuture<?> scheduledFuture;
    private boolean running;

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public MBeanDestinationDiscoverer getDiscoverer() {
        return discoverer;
    }

    public void setDiscoverer(MBeanDestinationDiscoverer discoverer) {
        this.discoverer = discoverer;
    }

    public long getInterPollDelay() {
        return interPollDelay;
    }

    public void setInterPollDelay(long interPollDelay) {
        this.interPollDelay = interPollDelay;
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public void start () {
        synchronized ( this ) {
            if ( this.running ) {
                throw new IllegalStateException("already running");
            }

            this.running = true;
        }

        ScheduledFuture<?> future = this.executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                executePoll();
            }
        }, 0, this.interPollDelay, TimeUnit.MILLISECONDS);

        synchronized ( this ) {
            // Check for stopped before finished starting

            if ( this.running ) {
                this.scheduledFuture = future;
            } else {
                future.cancel(true);
            }
        }
    }

    public void stop () {
        ScheduledFuture<?> toCancel = null;

        synchronized ( this ) {
            if ( ! this.running ) {
                return; // Not an error
            }

            this.running = false;

            if ( this.scheduledFuture != null ) {
                toCancel = this.scheduledFuture;
                this.scheduledFuture = null;
            }
        }

        if ( toCancel != null ) {
            toCancel.cancel(true);
        }
    }

    protected void executePoll () {
        try {
            this.discoverer.pollOnce();
        } catch (Exception exc) {
            this.log.warn("error on poll for destination updates", exc);
        }
    }
}
