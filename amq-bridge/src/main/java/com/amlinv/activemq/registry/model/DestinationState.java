package com.amlinv.activemq.registry.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by art on 5/2/15.
 */
public class DestinationState extends DestinationInfo {
    private final Map<String, PerBrokerInfo> brokerDetails = new HashMap<String, PerBrokerInfo>();

    public DestinationState(String name) {
        super(name);
    }

    public DestinationState(String name, String brokerName) {
        this(name);
        this.putBrokerInfo(brokerName, true);
    }

    public DestinationState(DestinationInfo dup) {
        super(dup.getName());
    }

    public void putBrokerInfo (String brokerId, boolean exists) {
        long timestamp = System.nanoTime();

        synchronized ( this.brokerDetails ) {
            //
            // If the destination no longer exists for the broker, keep the old timestamp, if available.
            //
            if ( ! exists ) {
                PerBrokerInfo info = this.brokerDetails.get(brokerId);
                if ( info != null ) {
                    timestamp = info.lastSeen;
                } else {
                    timestamp = -1;
                }
            }

            this.brokerDetails.put(brokerId, new PerBrokerInfo(timestamp, exists));
        }
    }

    /**
     * Determine if this destination currently is known to exist on any broker registered for the destination.
     *
     * @return true => the destination is known to exist on at least one broker; false => the destination is not known
     * to exist on any broker.
     */
    public boolean existsAnyBroker () {
        boolean result = false;

        synchronized ( this.brokerDetails ) {
            Iterator<PerBrokerInfo> iter = this.brokerDetails.values().iterator();

            while ( ( ! result ) && ( iter.hasNext() ) ) {
                result = iter.next().exists;
            }
        }

        return result;
    }

    /**
     * Keep details for the destination as it pertains to a single broker.
     */
    protected class PerBrokerInfo {
        public long lastSeen;
        public boolean exists;

        public PerBrokerInfo(long lastSeen, boolean exists) {
            this.lastSeen = lastSeen;
            this.exists = exists;
        }
    }
}
