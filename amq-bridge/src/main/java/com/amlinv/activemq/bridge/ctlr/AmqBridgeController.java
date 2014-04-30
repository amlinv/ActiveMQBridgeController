package com.amlinv.activemq.bridge.ctlr;

import static com.amlinv.activemq.bridge.ctlr.events.AmqBridgeEventSource.*;

import com.amlinv.activemq.bridge.ctlr.events.*;
import com.amlinv.activemq.bridge.engine.AmqBridge;
import com.amlinv.activemq.bridge.engine.AmqBridgeConfigurationException;
import com.amlinv.activemq.bridge.model.AmqBridgeSpec;
import com.amlinv.util.event.EventListenerAsyncUtil;
import com.amlinv.util.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.*;

/**
 * Controller for an ActiveMQ Bridge.
 *
 * Created by art on 4/19/14.
 */
public class AmqBridgeController implements Service {
    private static final Logger                 LOG = LoggerFactory.getLogger(AmqBridgeController.class);

    private final Map<String, AmqBridgeSpec>    bridgeSpecs = new HashMap<String, AmqBridgeSpec>();
    private Map<String, AmqBridge>              activeBridges = new HashMap<String, AmqBridge>();
    private final ServiceController             serviceCtlr = new ServiceController(this);
    private final EventListenerAsyncUtil        eventSendUtil =
                                                new EventListenerAsyncUtil("amq-bridge-event-sender-", 3, 5, 3000, 25);

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

    public void updateBridge (String id, AmqBridgeSpec spec)
    throws BridgeAlreadyExistsException, AmqBridgeConfigurationException, JMSException, BridgeNotDefinedException, BridgeActiveException {
        synchronized ( this.bridgeSpecs ) {
            //
            // Make sure the original spec exists and is not active, and make sure the new ID is not already used,
            //  unless this is an update to the same bridge.
            //
            //  The active check is to ensure a bridge's state does not become confused; this may be relaxes in the
            //  future (e.g. changing the ID or adding new destinations may be allowed).
            //
            if ( ! this.bridgeSpecs.containsKey(id) ) {
                throw new BridgeNotDefinedException(id);
            }
            if ( this.activeBridges.containsKey(id) ) {
                throw new BridgeActiveException(id);
            }
            if ( ! ( id.equals(spec.getId()) ) ) {
                if (this.bridgeSpecs.containsKey(spec.getId())) {
                    throw new BridgeAlreadyExistsException(spec.getId());
                }
            }

            // Replace the spec with the original ID with the spec with the new ID.
            this.bridgeSpecs.remove(id);
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
    throws BridgeNotDefinedException, BridgeNotActiveException, JMSException, AmqBridgeConfigurationException,
           BridgeControllerNotActiveException
    {
        //
        // Stop the bridge first, if it is active.  Use the unchecked version of the stop method as this operation must
        //  be safe whether the controller is active or not; while it is unlikely to need to stop the bridge while the
        //  service is inactive, it could happen due to race conditions (e.g. stop() is active while deleteBridge() and
        //  the stop handling race to stop the bridge).
        //
        // TBD: what happens if the stop fails because it is already stopped?
        //
        synchronized ( this.activeBridges ) {
            if ( this.activeBridges.containsKey(id) ) {
                this.stopBridgeUnchecked(id);
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

    public boolean  isBridgeIdle (String id) {
        synchronized ( this.bridgeSpecs ) {
            return  this.activeBridges.containsKey(id);
        }
    }

    public void startBridge (String id)
    throws AmqBridgeConfigurationException, JMSException, BridgeAlreadyActiveException, BridgeNotDefinedException,
           BridgeControllerNotActiveException
    {
        AmqBridgeSpec   spec;
        AmqBridge       bridge;

        this.checkActive();

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

        fireBridgeStartedEvent(id, new AmqBridgeEventCause("start requested", USER));

        LOG.info("bridge {} has been started", spec.getId());
    }

    public void stopBridge (String id)
    throws BridgeNotActiveException, AmqBridgeConfigurationException, JMSException, BridgeNotDefinedException,
           BridgeControllerNotActiveException
    {
        this.checkActive();
        this.stopBridgeUnchecked(id);
    }


    public void start () throws Exception {
        this.serviceCtlr.start();
    }

    public void stop () throws Exception {
        this.serviceCtlr.stop();
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

        this.eventSendUtil.shutdown();
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
        eventSendUtil.addListener(newListener);
    }

    public void removeListener (AmqBridgeListener rmListener) {
        eventSendUtil.removeListener(rmListener);
    }

    public boolean isRunning() {
        return this.serviceCtlr.isStarted();
    }

    protected void  checkActive () throws BridgeControllerNotActiveException {
        if ((!this.serviceCtlr.isStarted()) || (this.serviceCtlr.isStopping())) {
            throw new BridgeControllerNotActiveException();
        }
    }

    protected void stopBridgeUnchecked (String id)
    throws BridgeNotActiveException, AmqBridgeConfigurationException, JMSException, BridgeNotDefinedException,
           BridgeControllerNotActiveException
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

        this.stopOneBridge(id, bridge);

        LOG.info("bridge {} has been stopped", id);
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
            fireBridgeStoppedEvent(id, new AmqBridgeEventCause("stop requested", USER));
        } catch ( Exception jmsExc ) {
            LOG.info("error on stopping bridge {}", jmsExc);
        }
    }

    protected void  fireBridgeStoppedEvent (String id, AmqBridgeEventCause cause) {
        AmqBridgeEvent  stopEvent = new AmqBridgeEvent();

        stopEvent.setType(AmqBridgeEventType.BRIDGE_STOPPED);
        stopEvent.setCause(cause);
        stopEvent.setData(id);

        List<AmqBridgeListener> curListeners;
        eventSendUtil.queueEventSend(stopEvent);
    }

    protected void  fireBridgeStartedEvent (String id, AmqBridgeEventCause cause) {
        AmqBridgeEvent  startEvent = new AmqBridgeEvent();

        startEvent.setType(AmqBridgeEventType.BRIDGE_STARTED);
        startEvent.setCause(cause);
        startEvent.setData(id);

        this.eventSendUtil.queueEventSend(startEvent);
    }

}
