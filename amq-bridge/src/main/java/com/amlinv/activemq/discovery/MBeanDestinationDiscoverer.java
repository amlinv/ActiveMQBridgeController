package com.amlinv.activemq.discovery;

import com.amlinv.activemq.registry.DestinationRegistry;
import com.amlinv.activemq.registry.model.DestinationInfo;
import com.amlinv.jmxutil.connection.MBeanAccessConnection;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import com.amlinv.jmxutil.polling.JmxActiveMQUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Set;

/**
 * Created by art on 5/20/15.
 */
public class MBeanDestinationDiscoverer {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MBeanDestinationDiscoverer.class);

    private Logger log = DEFAULT_LOGGER;

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
    public MBeanDestinationDiscoverer(String destinationType) {
        this.destinationType = destinationType;
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

    public void pollOnce () throws IOException {
        MBeanAccessConnection connection = this.mBeanAccessConnectionFactory.createConnection();

        try {
            ObjectName destinationPattern =
                    JmxActiveMQUtil.getDestinationObjectName(this.brokerName, "*", this.destinationType);

            Set<ObjectName> found = connection.queryNames(destinationPattern, null);

            for ( ObjectName oneDestName : found ) {
                this.onFoundDestination(oneDestName);
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
     * @param destMBeanName name of the mbean representing the destination.
     */
    protected void onFoundDestination (ObjectName destMBeanName) {
        String destName = JmxActiveMQUtil.extractDestinationName(destMBeanName);

        if ( ( destName != null ) && ( ! destName.isEmpty() ) ) {
            this.registry.putIfAbsent(destName, new DestinationInfo(destName));
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
