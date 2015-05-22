package com.amlinv.activemq.discovery;

import com.amlinv.activemq.registry.DestinationRegistry;
import com.amlinv.activemq.registry.model.DestinationState;
import com.amlinv.jmxutil.connection.MBeanAccessConnection;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import com.amlinv.jmxutil.polling.JmxActiveMQUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by art on 5/20/15.
 */
public class MBeanDestinationDiscoverer {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MBeanDestinationDiscoverer.class);

    private Logger log = DEFAULT_LOGGER;

    private final String brokerId;

    private String brokerName = "*";
    private String destinationType;

    private DestinationRegistry registry;
    private MBeanAccessConnectionFactory mBeanAccessConnectionFactory;

    /**
     * Type of destination to discover, and which are maintained in the given registry.
     *
     * @param destinationType type of destination; must match them type in the MBean name (destinationType attribute):
     *                        Queue, Topic, etc.
     */
    public MBeanDestinationDiscoverer(String destinationType, String brokerId) {
        this.destinationType = destinationType;
        this.brokerId = brokerId;
    }

    public DestinationRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(DestinationRegistry registry) {
        this.registry = registry;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    public MBeanAccessConnectionFactory getmBeanAccessConnectionFactory() {
        return mBeanAccessConnectionFactory;
    }

    public void setmBeanAccessConnectionFactory(MBeanAccessConnectionFactory mBeanAccessConnectionFactory) {
        this.mBeanAccessConnectionFactory = mBeanAccessConnectionFactory;
    }

    /**
     * Poll for destinations and update the registry with new destinations found, and clean out destinations not found
     * and not existing on any broker.
     *
     * @throws IOException
     */
    public void pollOnce () throws IOException {
        MBeanAccessConnection connection = this.mBeanAccessConnectionFactory.createConnection();

        //
        // Set of Queues known at start in order to detect Queues lost.  This set may contain queues already known to
        //  have been lost.
        //
        Set<String> remainingQueues = new HashSet<>(this.registry.keys());

        try {
            ObjectName destinationPattern =
                    JmxActiveMQUtil.getDestinationObjectName(this.brokerName, "*", this.destinationType);

            Set<ObjectName> found = connection.queryNames(destinationPattern, null);

            //
            // Iterate over the mbean names matching the pattern, extract the destination name, and process.
            //
            for ( ObjectName oneDestOName : found ) {
                String destName = JmxActiveMQUtil.extractDestinationName(oneDestOName);

                this.onFoundDestination(destName);
                remainingQueues.remove(destName);
            }

            //
            // Mark any queues remaining in the expected queue set as not known by the broker.
            //
            for ( String missingQueue : remainingQueues ) {
                DestinationState destState = this.registry.get(missingQueue);
                if ( destState != null ) {
                    destState.putBrokerInfo(this.brokerId, false);

                    // Remove now if not known by any broker.
                    if ( ! destState.existsAnyBroker() ) {
                        this.registry.remove(missingQueue);
                    }
                }
            }
        } catch (MalformedObjectNameException monExc) {
            throw new RuntimeException("unexpected object name failure for destinationType=" + this.destinationType,
                    monExc);
        } finally {
            this.safeClose(connection);
        }
    }

    /**
     * For the destination represented by the named mbean, add the destination to the registry.
     *
     * @param destName name of the destination.
     */
    protected void onFoundDestination (String destName) {
        if ( ( destName != null ) && ( ! destName.isEmpty() ) ) {
            DestinationState destState =
                    this.registry.putIfAbsent(destName, new DestinationState(destName, this.brokerId));

            //
            // If it was already there, mark it as seen now by the broker.
            //
            if ( destState != null ) {
                destState.putBrokerInfo(this.brokerId, true);
            }
        }
    }

    protected void safeClose (MBeanAccessConnection connection) {
        try {
            connection.close();
        } catch ( IOException ioExc ) {
            log.warn("failed to close mbean access connection cleanly", ioExc);
        }
    }
}
