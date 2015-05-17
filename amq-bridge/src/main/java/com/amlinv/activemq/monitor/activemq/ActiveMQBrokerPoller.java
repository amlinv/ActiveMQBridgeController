package com.amlinv.activemq.monitor.activemq;

import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnectionFactory;
import com.amlinv.activemq.monitor.jmx.polling.JmxAttributePoller;
import com.amlinv.activemq.monitor.model.ActiveMQBrokerStats;
import com.amlinv.activemq.monitor.model.ActiveMQQueueStats;
import com.amlinv.activemq.monitor.model.BrokerStatsPackage;
import com.amlinv.logging.RepeatLogMessageSuppressor;
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

    private Logger STATS_LOG = LoggerFactory.getLogger("com.amlinv.activemq.monitor.activemq.statsLog");

    private final String brokerName;

    private Timer scheduler = new Timer();

    private Set<String> queueNames = new TreeSet<>();
    private Set<String> topicNames = new TreeSet<>();

    private long pollingInterval = 3000;

    private final MBeanAccessConnectionFactory mBeanAccessConnectionFactory;
    private final ActiveMQBrokerPollerListener listener;
    private MyJmxAttributePoller poller;

    private RepeatLogMessageSuppressor logThrottlePollFailure = new RepeatLogMessageSuppressor();

    private boolean pollActiveInd = false;
    private boolean started = false;
    private boolean stopped = false;

    public ActiveMQBrokerPoller(String brokerName, MBeanAccessConnectionFactory mBeanAccessConnectionFactory,
                                ActiveMQBrokerPollerListener listener) {

        this.brokerName = brokerName;
        this.mBeanAccessConnectionFactory = mBeanAccessConnectionFactory;
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

    public void removeMonitoredQueue (String name) {
        MyJmxAttributePoller newPoller = null;

        synchronized ( this ) {
            this.queueNames.remove(name);

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

    public void removeMonitoredTopic (String name) {
        MyJmxAttributePoller newPoller = null;

        synchronized ( this ) {
            this.topicNames.remove(name);

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
        newPoller.setmBeanAccessConnectionFactory(this.mBeanAccessConnectionFactory);

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

    protected void onPollComplete (MyJmxAttributePoller poller) {
        BrokerStatsPackage resultStorage = poller.getResultStorage();

        this.listener.onBrokerPollComplete(resultStorage);

        this.logStats(resultStorage);
    }

    protected void logStats (BrokerStatsPackage resultStorage) {
        ActiveMQBrokerStats brokerStats = resultStorage.getBrokerStats();
        if ( brokerStats != null ) {
            String line = formatBrokerStatsLogLine(brokerStats);

            this.STATS_LOG.info("{}", line.toString());
        }

        if ( resultStorage.getQueueStats() != null ) {
            for (Map.Entry<String, ActiveMQQueueStats> oneEntry : resultStorage.getQueueStats().entrySet()) {
                String line = formatQueueStatsLogLine(oneEntry.getKey(), oneEntry.getValue());

                this.STATS_LOG.info("{}", line.toString());
            }
        }
    }

    protected String formatBrokerStatsLogLine(ActiveMQBrokerStats brokerStats) {
        StringBuilder buffer = new StringBuilder();

        buffer.append("|broker-stats|");
        buffer.append(encodeLogStatString(brokerStats.getBrokerName()));
        buffer.append("|");
        buffer.append(brokerStats.getAverageMessageSize());
        buffer.append("|");
        buffer.append(encodeLogStatString(brokerStats.getUptime()));
        buffer.append("|");
        buffer.append(brokerStats.getUptimeMillis());
        buffer.append("|");
        buffer.append(brokerStats.getMemoryLimit());
        buffer.append("|");
        buffer.append(brokerStats.getMemoryPercentUsage());
        buffer.append("|");
        buffer.append(brokerStats.getCurrentConnectionsCount());
        buffer.append("|");
        buffer.append(brokerStats.getTotalConsumerCount());
        buffer.append("|");
        buffer.append(brokerStats.getTotalMessageCount());
        buffer.append("|");
        buffer.append(brokerStats.getTotalEnqueueCount());
        buffer.append("|");
        buffer.append(brokerStats.getTotalDequeueCount());
        buffer.append("|");

        return buffer.toString();
    }

    private String formatQueueStatsLogLine(String queueName, ActiveMQQueueStats stats) {
        StringBuilder buffer = new StringBuilder();

        buffer.append("|queue-stats|");
        buffer.append(encodeLogStatString(queueName));
        buffer.append("|");
        buffer.append(encodeLogStatString(stats.getBrokerName()));
        buffer.append("|");
        buffer.append(stats.getQueueSize());
        buffer.append("|");
        buffer.append(stats.getEnqueueCount());
        buffer.append("|");
        buffer.append(stats.getDequeueCount());
        buffer.append("|");
        buffer.append(stats.getNumConsumers());
        buffer.append("|");
        buffer.append(stats.getNumProducers());
        buffer.append("|");
        buffer.append(stats.getCursorPercentUsage());
        buffer.append("|");
        buffer.append(stats.getMemoryPercentUsage());
        buffer.append("|");
        buffer.append(stats.getInflightCount());

        return buffer.toString();
    }

    protected String encodeLogStatString (String orig) {
        return  orig.replaceAll("[|]", "%v%");
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
