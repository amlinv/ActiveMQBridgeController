package com.amlinv.activemq.monitor.web;

import com.amlinv.activemq.discovery.MBeanDestinationDiscoverer;
import com.amlinv.activemq.discovery.MBeanDestinationDiscovererScheduler;
import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPoller;
import com.amlinv.activemq.registry.model.DestinationState;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import com.amlinv.jmxutil.polling.JmxActiveMQUtil;
import com.amlinv.activemq.registry.BrokerRegistry;
import com.amlinv.activemq.registry.BrokerRegistryListener;
import com.amlinv.activemq.registry.DestinationRegistry;
import com.amlinv.activemq.registry.model.BrokerInfo;
import com.amlinv.activemq.registry.model.DestinationInfo;
import com.amlinv.util.thread.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by art on 3/31/15.
 */
@Path("/monitor")
public class MonitorWebController {
    private static final Logger LOG = LoggerFactory.getLogger(MonitorWebController.class);

    private final MyBrokerRegistryListener myBrokerRegistryListener;

    private BrokerRegistry brokerRegistry;
    private DestinationRegistry queueRegistry;
    private DestinationRegistry topicRegistry;

    private Map<String, ActiveMQBrokerPoller> brokerPollerMap;
    private Map<String, MBeanDestinationDiscovererScheduler> queueDiscoverers;
    private AtomicBoolean started = new AtomicBoolean(false);

    private boolean autoStart = true;
    private boolean autoDiscoverQueues = true;

    private ScheduledExecutorService discovererExecutorService =
            new ScheduledThreadPoolExecutor(5, new DaemonThreadFactory("discoverer-polling-thread-"));

    private MonitorWebsocketBrokerStatsFeed websocketBrokerStatsFeed;

    public MonitorWebController() {
        this.myBrokerRegistryListener = new MyBrokerRegistryListener();

        this.brokerPollerMap = new TreeMap<>();
        this.queueDiscoverers = new HashMap<>();
    }

    public BrokerRegistry getBrokerRegistry() {
        return brokerRegistry;
    }

    public void setBrokerRegistry(BrokerRegistry brokerRegistry) {
        this.brokerRegistry = brokerRegistry;
    }

    public DestinationRegistry getQueueRegistry() {
        return queueRegistry;
    }

    public void setQueueRegistry(DestinationRegistry queueRegistry) {
        this.queueRegistry = queueRegistry;
    }

    public MonitorWebsocketBrokerStatsFeed getWebsocketBrokerStatsFeed() {
        return websocketBrokerStatsFeed;
    }

    public void setWebsocketBrokerStatsFeed(MonitorWebsocketBrokerStatsFeed websocketBrokerStatsFeed) {
        this.websocketBrokerStatsFeed = websocketBrokerStatsFeed;
    }

    public MyBrokerRegistryListener getBrokerRegistryListener() {
        return myBrokerRegistryListener;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isAutoDiscoverQueues() {
        return autoDiscoverQueues;
    }

    public void setAutoDiscoverQueues(boolean autoDiscoverQueues) {
        this.autoDiscoverQueues = autoDiscoverQueues;
    }

    public void init () {
        LOG.info("Initializing monitor web controller");
        if ( this.autoStart ) {
            LOG.info("Starting monitoring now");
            this.startMonitoring();
        }
    }

    // TBD: make sure nothing new gets added during shutdown
    public void shutdown () {
        synchronized ( this.queueDiscoverers ) {
            for (MBeanDestinationDiscovererScheduler oneDiscovererScheduler : this.queueDiscoverers.values()) {
                oneDiscovererScheduler.stop();
            }
        }

        synchronized ( this.brokerPollerMap ) {
            for ( ActiveMQBrokerPoller onePoller : this.brokerPollerMap.values() ) {
                onePoller.stop();
            }
        }

        this.discovererExecutorService.shutdown();
    }

    @GET
    @Path("/brokers")
    public List<BrokerInfo> listMonitoredBrokers() {
        LOG.debug("listMonitoredBrokers");

        return new LinkedList<BrokerInfo>(this.brokerRegistry.values());
    }

    @PUT
    @Path("/broker")
    @Produces({ "application/json", "application/xml", "text/plain" })
    @Consumes({ "application/json", "application/xml", "application/x-www-form-urlencoded" })
    public String addBroker (@FormParam("brokerName") String brokerName, @FormParam("address") String address)
            throws Exception {

        return prepareBrokerPoller(brokerName, address);
    }

    @DELETE
    @Path("/broker")
    @Produces("text/plain")
    public String removeBrokerForm (@FormParam("address") @QueryParam("address") String address) {
        String result;

        result = performBrokerRemoval(address);

        return  result;
    }

    protected String performBrokerRemoval(String address) {
        String result;
        ActiveMQBrokerPoller removedPoller;

        // TBD222: stop using the address as the key since more than one broker may live at an address
        this.brokerRegistry.remove(address);

        // TBD: both brokerPollerMap and locations should be performed in a single atomic update, not separate atomic updates
        // TBD: one address can have more than one broker
        synchronized ( this.brokerPollerMap ) {
            removedPoller = this.brokerPollerMap.remove(address);
        }

        if ( removedPoller != null ) {
            result = "removed";
            removedPoller.stop();
        } else {
            result = "not found";
        }
        return result;
    }

    @PUT
    @Path("/queue")
    @Consumes({ "application/json", "application/xml", "application/x-www-form-urlencoded" })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addQueue (@FormParam("queueName") String queueName,
                              @DefaultValue("*") @FormParam("brokerName") String queryBroker,
                              @DefaultValue("*") @FormParam("address") String address) throws Exception {

        Set<String> additionalQueueNames;
        if ( queueName.endsWith("*") ) {
            additionalQueueNames = queryQueueNames(address, queryBroker, queueName);
        } else {
            additionalQueueNames = new TreeSet<>();
            additionalQueueNames.add(queueName);
        }

        for ( String oneAdditionalQueueName : additionalQueueNames ) {
            performQueueAdd(oneAdditionalQueueName);
        }

        Response response = Response.ok(additionalQueueNames).build();

        return  response;
    }

    @DELETE
    @Path("/queue")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response removeQueue (@FormParam("queueName") String queueName,
                            @DefaultValue("*") @FormParam("brokerName") String queryBroker,
                            @DefaultValue("*") @FormParam("address") String address) throws Exception {

        Set<String> removeQueueNames;
        if ( queueName.endsWith("*") ) {
            removeQueueNames = queryQueueNames(address, queryBroker, queueName);
        } else {
            removeQueueNames = new TreeSet<>();
            removeQueueNames.add(queueName);
        }

        for ( String rmQueueName : removeQueueNames ) {
            this.queueRegistry.remove(rmQueueName);
        }

        Response response = Response.ok(removeQueueNames).build();

        return  response;
    }

    @GET
    @Path("/start")
    @Produces("text/plain")
    public String requestStartMonitoring() throws Exception {
        String result;

        result = startMonitoring();

        return  result;
    }

    @GET
    @Path("/queryBrokers")
    @Produces({ "application/json", "application/xml" })
    public String[] queryBrokerNames(@QueryParam("address") String address) throws Exception {
        return  JmxActiveMQUtil.queryBrokerNames(address);
    }

    /**
     * Start monitoring now.
     *
     * @return text describing the result.
     */
    protected String startMonitoring() {
        String result;
        if ( ! this.started.getAndSet(true) ) {
            synchronized ( this.brokerPollerMap ) {
                for (ActiveMQBrokerPoller onePoller : this.brokerPollerMap.values()) {
                    onePoller.start();
                }
            }

            result = "started";
        } else {
            result = "already running";
        }
        return result;
    }

    protected void performQueueAdd(String queueName) {
        this.queueRegistry.putIfAbsent(queueName, new DestinationState(queueName));
    }

    /**
     * Prepare polling for the named broker at the given polling address.
     *
     * @param brokerName
     * @param address
     * @return
     * @throws Exception
     */
    protected String prepareBrokerPoller(String brokerName, String address) throws Exception {
        MBeanAccessConnectionFactory mBeanAccessConnectionFactory =
                JmxActiveMQUtil.getLocationConnectionFactory(address);

        if ( brokerName.equals("*") ) {
            String[] brokersAtLocation = JmxActiveMQUtil.queryBrokerNames(address);
            if ( brokersAtLocation == null ) {
                throw new Exception("unable to locate broker at " + address);
            } else if ( brokersAtLocation.length != 1 ) {
                throw new Exception("found more than one broker at " + address + "; count=" + brokersAtLocation.length);
            } else {
                brokerName = brokersAtLocation[0];
            }
        }

        this.brokerRegistry.put(address, new BrokerInfo("unknown-broker-id", brokerName, "unknown-broker-url"));

        ActiveMQBrokerPoller brokerPoller =
                new ActiveMQBrokerPoller(brokerName, mBeanAccessConnectionFactory, this.websocketBrokerStatsFeed);

        brokerPoller.setQueueRegistry(this.queueRegistry);
        brokerPoller.setTopicRegistry(this.topicRegistry);

        // TBD: one automic update for brokerPollerMap and locations (is there an echo in here?)
        synchronized ( this.brokerPollerMap ) {
            if ( ! this.brokerPollerMap.containsKey(address) ) {
                this.brokerPollerMap.put(address, brokerPoller);
            } else {
                LOG.info("ignoring duplicate add of broker address {}", address);
                return "already exists";
            }
        }

        // No need to synchronize to avoid races here; the poller will not start if either already started, or already
        //  stopped.
        if ( this.started.get() ) {
            brokerPoller.start();
        }

        // Add auto-discovery of Queues for this broker, if enabled
        if ( this.autoDiscoverQueues ) {
            this.prepareBrokerQueueDiscoverer(brokerName, address, mBeanAccessConnectionFactory);
        }

        return address + " = " + brokerName;
    }

    protected void prepareBrokerQueueDiscoverer (String brokerName, String address,
                                                 MBeanAccessConnectionFactory connectionFactory) {

        MBeanDestinationDiscoverer discoverer = new MBeanDestinationDiscoverer("Queue", address);
        discoverer.setmBeanAccessConnectionFactory(connectionFactory);
        discoverer.setBrokerName(brokerName);
        discoverer.setRegistry(this.queueRegistry);

        MBeanDestinationDiscovererScheduler scheduler = new MBeanDestinationDiscovererScheduler();

        scheduler.setExecutor(this.discovererExecutorService);
        scheduler.setDiscoverer(discoverer);

        scheduler.start();

        synchronized ( this.queueDiscoverers ) {
            this.queueDiscoverers.put(address, scheduler);
        }
    }

    // TBD: don't create mutliple JMX connections when performing multiple queries (JMX connector pool?)
    protected Set<String> queryQueueNames (String location, String brokerName, String queueNamePattern) throws Exception {
        Set<String> result = new TreeSet<>();

        if ( location.equals("*") ) {
            // TBD222: stop using address (aka location) as the registry key
            for ( String oneLocation : this.brokerRegistry.keys() ) {
                result.addAll(this.queryQueueNames(oneLocation, brokerName, queueNamePattern)); // RECURSION
            }
        } else {
            if ( brokerName.equals("*") ) {
                String[] brokerNames = this.queryBrokerNames(location);

                for ( String oneBrokerName : brokerNames ) {
                    result.addAll(this.queryQueueNames(location, oneBrokerName, queueNamePattern)); // RECURSION
                }
            } else {
                String[] names = JmxActiveMQUtil.queryQueueNames(location, brokerName, queueNamePattern);
                result.addAll(Arrays.asList(names));
            }
        }

        return  result;
    }

    /**
     * Listener for events representing a change in the list of monitored brokers.
     */
    protected class MyBrokerRegistryListener implements BrokerRegistryListener {
        /**
         * New broker was added to the registry; prepare polling of the broker.
         *
         * @param putKey key of the entry that was added to the registry.
         * @param putValue value of the entry that was added to the registry.
         */
        @Override
        public void onPutEntry(String putKey, BrokerInfo putValue) {
            try {
                prepareBrokerPoller(putValue.getBrokerName(), putKey);
            } catch (Exception exc) {
                LOG.error("Failed to prepare polling for broker: brokerName={}; address={}", putValue.getBrokerName(),
                        putKey, exc);
            }
        }

        /**
         * An existing broker was removed from the registry; stop polling the broker.
         *
         * @param removeKey key of the entry that was removed.
         * @param removeValue value of the entry that was removed.
         */
        @Override
        public void onRemoveEntry(String removeKey, BrokerInfo removeValue) {
            performBrokerRemoval(removeKey);
        }

        /**
         * An existing broker was modified in the registry; nothing to do here.
         *
         * @param replaceKey
         * @param oldValue
         * @param newValue
         */
        @Override
        public void onReplaceEntry(String replaceKey, BrokerInfo oldValue, BrokerInfo newValue) {
        }
    }
}
