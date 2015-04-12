package com.amlinv.activemq.monitor.activemq;

import com.amlinv.activemq.monitor.jmx.connection.JMXConnectionSource;
import com.amlinv.activemq.monitor.jmx.polling.JmxAttributePoller;
import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.activemq.monitor.web.MonitorWebsocket;
import com.amlinv.logging.RepeatLogMessageSuppressor;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * TBD: use immutable copies of the data
 * Created by art on 4/2/15.
 */
public class ActiveMQBrokerPoller {
    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQBrokerPoller.class);

    private final String brokerName;

    private Timer scheduler = new Timer();

    private Set<String> queueNames = new TreeSet<>();
    private Set<String> topicNames = new TreeSet<>();

    private long pollingInterval = 1000;

    private final JMXConnectionSource jmxConnectionSource;
    private final ActiveMQBrokerPollerListener listener;
    private MyJmxAttributePoller poller;

    private RepeatLogMessageSuppressor logThrottlePollFailure = new RepeatLogMessageSuppressor();

    private boolean pollActiveInd = false;
    private boolean started = false;
    private boolean stopped = false;

    public ActiveMQBrokerPoller(String brokerName, JMXConnectionSource jmxConnectionSource,
                                ActiveMQBrokerPollerListener listener) {

        this.brokerName = brokerName;
        this.jmxConnectionSource = jmxConnectionSource;
        this.listener = listener;
    }

    public void addMonitoredQueue (String name) {
        MyJmxAttributePoller newPoller = null;

        synchronized ( this ) {
            this.queueNames.add(name);

            if ( this.started ) {
                newPoller = this.prepareNewPoller();
            }
        }

        if ( newPoller != null ) {
            activateNewPoller(newPoller);
        }
    }

    public void addMonitoredTopic (String name) {
        MyJmxAttributePoller newPoller = null;

        synchronized ( this ) {
            this.topicNames.add(name);

            if ( this.started ) {
                newPoller = this.prepareNewPoller();
            }
        }

        if ( newPoller != null ) {
            activateNewPoller(newPoller);
        }
    }

    public void start () {
        synchronized ( this ) {
            if ( this.started || this.stopped ) {
                return;
            }

            this.started = true;
            this.poller = this.prepareNewPoller();
        }

        PollerTask pollerTask = new PollerTask();
        this.scheduler.schedule(pollerTask, 0, pollingInterval);
    }

    public void stop () {
        this.scheduler.cancel();

        this.stopped = true;

        synchronized ( this ) {
            this.notifyAll();
        }
    }

    public void waitUntilStopped () throws InterruptedException {
        synchronized ( this ) {
            // Wait until stop is called.
            while ( ! stopped ) {
                this.wait();
            }

            // Then wait for any active poll to actually complete.
            while ( pollActiveInd ) {
                this.wait();
            }
        }
    }

    protected MyJmxAttributePoller prepareNewPoller() {
        BrokerStatsPackage resultStorage = this.preparePolledResultStorage();
        List<Object> polled = this.preparePolledObjects(resultStorage);

        MyJmxAttributePoller newPoller = new MyJmxAttributePoller(polled, resultStorage);
        newPoller.setJmxConnectionSource(this.jmxConnectionSource);

        return  newPoller;
    }

    protected BrokerStatsPackage preparePolledResultStorage () {
        Map<String, ActiveMQQueueStats> queueStatsMap = new TreeMap<>();

        for ( String oneQueueName : queueNames ) {
            ActiveMQQueueStats queueStats = new ActiveMQQueueStats(this.brokerName, oneQueueName);

            queueStatsMap.put(oneQueueName, queueStats);
        }

        BrokerStatsPackage result = new BrokerStatsPackage(new ActiveMQBrokerStats(this.brokerName), queueStatsMap);
        // TBD: add Topics to monitor

        return  result;
    }

    protected List<Object> preparePolledObjects (BrokerStatsPackage resultStorage) {
        List<Object> result = new LinkedList<>();

        result.add(resultStorage.getBrokerStats());

        for ( ActiveMQQueueStats oneQueueStats : resultStorage.getQueueStats().values() ) {
            result.add(oneQueueStats);
        }

        // TBD: add Topics to monitor

        return  result;
    }

    protected void activateNewPoller (MyJmxAttributePoller newPoller) {
        MyJmxAttributePoller oldPoller = this.poller;
        this.poller = newPoller;
        if ( oldPoller != null ) {
            oldPoller.shutdown();
        }
    }

    protected void pollOnce () {
        MyJmxAttributePoller pollerSnapshot = this.poller;

        try {
            pollActiveInd = true;
            pollerSnapshot.poll();
        } catch ( IOException ioExc ) {
            this.logThrottlePollFailure.warn(LOG, "poll of broker {} failed", this.brokerName, ioExc);
        } finally {
            synchronized ( this ) {
                pollActiveInd = false;
                this.notifyAll();
            }
        }

        this.onPollComplete(pollerSnapshot);
    }

    // TBD: use a listener instead of directly calling into MonitorWebsocket
    protected void onPollComplete (MyJmxAttributePoller poller) {
        BrokerStatsPackage resultStorage = poller.getResultStorage();
        this.listener.onBrokerPollComplete(resultStorage);
    }

    protected class PollerTask extends TimerTask {
        @Override
        public void run() {
            pollOnce();
        }
    }

    protected static class MyJmxAttributePoller extends JmxAttributePoller {
        private BrokerStatsPackage resultStorage;

        public MyJmxAttributePoller(List<Object> polledObjects, BrokerStatsPackage resultStorage) {
            super(polledObjects);

            this.resultStorage = resultStorage;
        }

        public BrokerStatsPackage getResultStorage() {
            return resultStorage;
        }
    }
}
