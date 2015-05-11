package com.amlinv.activemq.monitor.web;

import com.amlinv.activemq.monitor.activemq.ActiveMQBrokerPoller;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnectionFactory;
import com.amlinv.activemq.monitor.jmx.polling.JmxActiveMQUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.*;

/**
 * Created by art on 3/31/15.
 */
@Path("/monitor")
public class MonitorWebController {
    private static final Logger LOG = LoggerFactory.getLogger(MonitorWebController.class);

    private Set<String> monitoredBrokers;
    private Set<String> queueNames;
    private Set<String> topicNames;
    private Set<String> locations;
    private Map<String, ActiveMQBrokerPoller> brokerPollerMap;
    private AtomicBoolean started = new AtomicBoolean(false);

    public MonitorWebController() {
        this.monitoredBrokers = new TreeSet<String>();
        this.queueNames = new TreeSet<>();
        this.topicNames = new TreeSet<>();
        this.locations = new TreeSet<>();

        this.brokerPollerMap = new TreeMap<>();
    }

    @GET
    @Path("/brokers")
    public List<String> listMonitoredBrokers() {
        LOG.debug("listMonitoredBrokers");

        return new LinkedList<String>(this.monitoredBrokers);
    }

    @PUT
    @Path("/broker")
    @Produces({ "application/json", "application/xml", "text/plain" })
    @Consumes({ "application/json", "application/xml", "application/x-www-form-urlencoded" })
    public String addBroker (@FormParam("brokerName") String brokerName, @FormParam("address") String address)
            throws Exception {

        this.monitoredBrokers.add(address);

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

        ActiveMQBrokerPoller brokerPoller = new ActiveMQBrokerPoller(brokerName, mBeanAccessConnectionFactory,
                new MonitorWebsocket.MyBrokerPollerListener());

        synchronized ( this.queueNames ) {
            for ( String oneQueueName : this.queueNames ) {
                brokerPoller.addMonitoredQueue(oneQueueName);
            }
        }

        // TBD: one automic update for brokerPollerMap and locations (is there an echo in here?)
        synchronized ( this.brokerPollerMap ) {
            if ( ! this.brokerPollerMap.containsKey(address) ) {
                this.brokerPollerMap.put(address, brokerPoller);
            } else {
                LOG.info("ignoring duplicate add of broker address {}", address);
                return "already exists";
            }
        }

        synchronized ( this.locations ) {
            this.locations.add(address);
        }

        // No need to synchronize to avoid races here; the poller will not start if either already started, or already
        //  stopped.
        if ( this.started.get() ) {
            brokerPoller.start();
        }

        return address + " = " + brokerName;
    }

    @DELETE
    @Path("/broker")
    @Produces("text/plain")
    public String removeBrokerForm (@FormParam("address") @QueryParam("address") String address) {
        String result;

        result = performBrokerRemoval(address);

        return  result;
    }

    private String performBrokerRemoval(String address) {
        String result;ActiveMQBrokerPoller removedPoller;

        // TBD: both brokerPollerMap and locations should be performed in a single atomic update, not separate atomic updates
        // TBD: one address can have more than one broker
        synchronized ( this.brokerPollerMap ) {
            removedPoller = this.brokerPollerMap.remove(address);
        }

        synchronized ( this.locations ) {
            this.locations.remove(address);
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
    @Produces("text/plain")
    public String addQueue (@FormParam("queueName") String queueName,
                            @DefaultValue("*") @FormParam("brokerName") String queryBroker,
                            @DefaultValue("*") @FormParam("address") String address) throws Exception {

        Set<String> additionalQueueNames;
        if ( queueName.endsWith("*") ) {
            additionalQueueNames = queryQueueNames(address, queryBroker, queueName);
        } else {
            additionalQueueNames = new TreeSet<>();
            additionalQueueNames.add(queueName);
        }

        synchronized ( this.queueNames ) {
            this.queueNames.addAll(additionalQueueNames);
        }

        synchronized ( this.brokerPollerMap ) {
            for (ActiveMQBrokerPoller oneBrokerPoller : this.brokerPollerMap.values() ) {
                for ( String oneQueueName : additionalQueueNames ) {
                    oneBrokerPoller.addMonitoredQueue(oneQueueName);
                }
            }
        }

        return  "added";
    }

    @DELETE
    @Path("/queue")
    @Produces("text/plain")
    public String removeQueue (@FormParam("queueName") String queueName,
                            @DefaultValue("*") @FormParam("brokerName") String queryBroker,
                            @DefaultValue("*") @FormParam("address") String address) throws Exception {

        Set<String> additionalQueueNames;
        if ( queueName.endsWith("*") ) {
            additionalQueueNames = queryQueueNames(address, queryBroker, queueName);
        } else {
            additionalQueueNames = new TreeSet<>();
            additionalQueueNames.add(queueName);
        }

        synchronized ( this.queueNames ) {
            this.queueNames.removeAll(additionalQueueNames);
        }

        synchronized ( this.brokerPollerMap ) {
            for (ActiveMQBrokerPoller oneBrokerPoller : this.brokerPollerMap.values() ) {
                for ( String oneQueueName : additionalQueueNames ) {
                    oneBrokerPoller.removeMonitoredQueue(oneQueueName);
                }
            }
        }

        return  "added";
    }

    @GET
    @Path("/start")
    @Produces("text/plain")
    public String startMonitoring () throws Exception {
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

        return  result;
    }

    @GET
    @Path("/queryBrokers")
    @Produces({ "application/json", "application/xml" })
    public String[] queryBrokerNames(@QueryParam("address") String address) throws Exception {
        return  JmxActiveMQUtil.queryBrokerNames(address);
    }

    // TBD: don't create mutliple JMX connections when performing multiple queries (JMX connector pool?)
    protected Set<String> queryQueueNames (String location, String brokerName, String queueNamePattern) throws Exception {
        Set<String> result = new TreeSet<>();

        if ( location.equals("*") ) {
            for ( String oneLocation : locations ) {
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
}
