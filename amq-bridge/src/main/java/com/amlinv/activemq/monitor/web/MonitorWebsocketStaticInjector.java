package com.amlinv.activemq.monitor.web;

import com.amlinv.javasched.Scheduler;

/**
 * Spring injection class for getting the websocket registry into the MonitorWebsocket.
 *
 * Created by art on 5/14/15.
 */
public class MonitorWebsocketStaticInjector {
    public MonitorWebsocketRegistry getRegistry() {
        return MonitorWebsocket.getRegistry();
    }

    public void setRegistry(MonitorWebsocketRegistry registry) {
        MonitorWebsocket.setRegistry(registry);
    }

    public long getSendTimeout () {
        return MonitorWebsocket.getSendTimeout();
    }

    public void setSendTimeout (long newTimeout) {
        MonitorWebsocket.setSendTimeout(newTimeout);
    }

    public void setScheduler(Scheduler newScheduler) {
        MonitorWebsocket.setScheduler(newScheduler);
    }
}
