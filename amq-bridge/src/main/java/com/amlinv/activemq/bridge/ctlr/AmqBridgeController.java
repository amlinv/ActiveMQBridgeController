package com.amlinv.activemq.bridge.ctlr;

import com.amlinv.activemq.bridge.engine.AmqBridge;
import com.amlinv.activemq.bridge.engine.AmqBridgeConfigurationException;
import com.amlinv.activemq.bridge.model.AmqBridgeSpec;
import com.amlinv.activemq.bridge.model.AmqBridgeListener;
import com.amlinv.util.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Controller for an ActiveMQ Bridge.
 *
 * Created by art on 4/19/14.
 */
public class AmqBridgeController implements Service {
    private static final Logger                 LOG = LoggerFactory.getLogger(AmqBridgeController.class);

    private final Map<String, AmqBridgeSpec>    bridgeSpecs = new HashMap<String, AmqBridgeSpec>();
    private Map<String, AmqBridge>              activeBridges = new HashMap<String, AmqBridge>();
    private final List<AmqBridgeListener>       listeners = new LinkedList<AmqBridgeListener>();
    private final ServiceController             serviceCtlr = new ServiceController(this);

    public Map<String, AmqBridgeSpec> getBridgeSpecs() {
        return new HashMap<String, AmqBridgeSpec>(this.bridgeSpecs);
    }

    public void addBridge (AmqBridgeSpec spec)
    throws BridgeAlreadyExistsException, AmqBridgeConfigurationException, JMSException
    {
        synchronized ( this.bridgeSpecs ) {
            if ( this.bridgeSpecs.containsKey(spec.getId()) ) {
                throw new BridgeAlreadyExistsException(spec.getId());
            }
            this.bridgeSpecs.put(spec.getId(), spec);
        }
    }

    /**
     * Delete the bridge with the given ID, stopping it if currently active.
     *
     * @param id
     * @throws BridgeNotDefinedException
     * @throws BridgeNotActiveException
     * @throws JMSException
     * @throws AmqBridgeConfigurationException
     */
    public void deleteBridge (String id)
    throws BridgeNotDefinedException, BridgeNotActiveException, JMSException, AmqBridgeConfigurationException
    {
        //
        // Stop the bridge first, if it is active.
        //
        synchronized ( this.activeBridges ) {
            if ( this.activeBridges.containsKey(id) ) {
                this.stopBridge(id);
            }
        }

        //
        // Now remove the bridge specification.  Note that it is possible for the same call to both stop the bridge
        //  above and thrown BridgeNotDefinedException below if another thread is actively attempting to delete the
        //  same bridge, and that's alright.  Either way, one of the two threads will throw BridgeNotDefinedException
        //  and one of the two will stop the bridge - there's really not much need to ensure they are not the same
        //  thread.
        //
        synchronized ( this.bridgeSpecs ) {
            if ( ! this.bridgeSpecs.containsKey(id) ) {
                throw new BridgeNotDefinedException(id);
            }

            this.bridgeSpecs.remove(id);
        }
    }

    public void start () throws Exception {
        this.serviceCtlr.start();
    }

    public void stop () throws Exception {
        this.serviceCtlr.stop();
    }

    public void startBridge (String id)
    throws AmqBridgeConfigurationException, JMSException, BridgeAlreadyActiveException, BridgeNotDefinedException
    {
        AmqBridgeSpec   spec;
        AmqBridge       bridge;

        LOG.info("starting bridge {}", id);

        synchronized ( this.bridgeSpecs ) {
            spec = this.bridgeSpecs.get(id);
            if (spec == null) {
                throw new BridgeNotDefinedException(id);
            }
        }

        synchronized ( this.activeBridges ) {
            if (this.activeBridges.containsKey(id)) {
                throw new BridgeAlreadyActiveException();
            }

            bridge = new AmqBridge();
            this.activeBridges.put(id, bridge);
        }

        boolean complete = false;
        try {
            bridge.setAmqBridgeSpec(spec);
            bridge.start();
            complete = true;
        } finally {
            if ( ! complete ) {
                synchronized ( this.activeBridges ) {
                    this.activeBridges.remove(id);
                }
            }
        }

        LOG.info("bridge {} has been started", spec.getId());
    }

    public void stopBridge (String id)
    throws BridgeNotActiveException, AmqBridgeConfigurationException, JMSException, BridgeNotDefinedException
    {
        AmqBridgeSpec   spec;
        AmqBridge       bridge;

        LOG.info("stopping bridge {}", id);

        synchronized ( this.bridgeSpecs ) {
            spec = this.bridgeSpecs.get(id);
            if (spec == null) {
                throw new BridgeNotDefinedException(id);
            }
        }

        synchronized ( this.activeBridges ) {
            if ( ! this.activeBridges.containsKey(id) ) {
                throw new BridgeNotActiveException(id);
            }

            bridge = this.activeBridges.remove(id);
        }

        bridge.stop();

        LOG.info("bridge {} has been stopped", spec.getId());
    }

    /**
     * Stop the service, stopping all active bridges in the process.
     *
     * @throws Exception
     */
    @Override
    public void stopService () throws Exception {
        Map<String, AmqBridge>  bridgesToStop;

        synchronized ( this.bridgeSpecs ) {
            bridgesToStop = this.activeBridges;
            this.activeBridges = new HashMap<String, AmqBridge>();
        }

        for ( Map.Entry<String, AmqBridge> oneEnt : bridgesToStop.entrySet() ) {
            stopOneBridge(oneEnt.getKey(), oneEnt.getValue());
        }
    }

    /**
     * Start the service; this is a no-op as individual bridges are started separately.  This will likely change in
     * the future to start up persisted bridges configured to automatically start.
     *
     * @throws Exception
     */
    @Override
    public void startService () throws Exception {
    }

    @Override
    public boolean  canRunMultipleTimes () {
        return  true;
    }

    @Override
    public String   getServiceName () {
        return  "AMQ Bridge Controller";
    }

    public void addListener (AmqBridgeListener newListener) {
        synchronized ( listeners ) {
            this.listeners.add(newListener);
        }
    }

    public void removeListener (AmqBridgeListener newListener) {
        synchronized ( listeners ) {
            this.listeners.remove(newListener);
        }
    }

    public boolean isRunning() {
        return this.serviceCtlr.isStarted();
    }

    /**
     * Stop the given bridge.
     *
     * @param bridge - the bridge to stop.
     */
    protected void  stopOneBridge (String id, AmqBridge bridge) {
        LOG.info("stopping bridge {}", id);
        try {
            bridge.stop();
        } catch ( Exception jmsExc ) {
            LOG.info("error on stopping bridge {}", jmsExc);
        }
    }
}
