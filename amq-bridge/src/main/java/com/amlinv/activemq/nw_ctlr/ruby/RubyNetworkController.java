package com.amlinv.activemq.nw_ctlr.ruby;

import com.amlinv.activemq.bridge.ctlr.AmqBridgeController;
import com.amlinv.activemq.bridge.ctlr.BridgeActiveException;
import com.amlinv.activemq.bridge.ctlr.BridgeAlreadyActiveException;
import com.amlinv.activemq.bridge.ctlr.BridgeAlreadyExistsException;
import com.amlinv.activemq.bridge.ctlr.BridgeControllerNotActiveException;
import com.amlinv.activemq.bridge.ctlr.BridgeNotActiveException;
import com.amlinv.activemq.bridge.ctlr.BridgeNotDefinedException;
import com.amlinv.activemq.bridge.engine.AmqBridgeConfigurationException;
import com.amlinv.activemq.bridge.model.AmqBridgeSpec;
import com.amlinv.activemq.bridge.model.impl.AmqBridgeSpecImpl;
import com.amlinv.activemq.nw_ctlr.NetworkController;
import com.amlinv.activemq.nw_ctlr.NetworkEvent;
import com.amlinv.activemq.nw_ctlr.NetworkEventListener;
import com.amlinv.activemq.nw_ctlr.NetworkEventSource;
import com.amlinv.activemq.nw_ctlr.event.AddConsumerEvent;
import com.amlinv.activemq.nw_ctlr.event.RemoveConsumerEvent;
import com.amlinv.activemq.registry.OldBrokerRegistry;
import com.amlinv.activemq.topo.registry.DestinationRegistry;
import com.amlinv.activemq.topo.registry.model.BrokerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by art on 5/2/15.
 */
public class RubyNetworkController implements NetworkController {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RubyNetworkController.class);

    private Logger log = DEFAULT_LOGGER;

    private final AmqBridgeController bridgeController;
    private final List<NetworkEventSource> networkEventSources;
    private final MyNetworkEventListener myNetworkEventListener = new MyNetworkEventListener();

    private final OldBrokerRegistry oldBrokerRegistry;
    private final DestinationRegistry queueRegistry;
    private final DestinationRegistry topicRegistry;

    private boolean started = false;

    public RubyNetworkController(AmqBridgeController bridgeController, List<NetworkEventSource> sources,
                                 OldBrokerRegistry initOldBrokerRegistry, DestinationRegistry initQueueRegistry,
                                 DestinationRegistry initTopicRegistry) {

        this.bridgeController = bridgeController;
        this.networkEventSources = new LinkedList<>(sources);
        this.oldBrokerRegistry = initOldBrokerRegistry;
        this.queueRegistry = initQueueRegistry;
        this.topicRegistry = initTopicRegistry;
    }

    // TBD: use ServiceController
    @Override
    public void start() {
        List<NetworkEventSource> sourcesNeedToStart;
        synchronized (this) {
            if (started) {
                this.log.info("ignoring duplicate attempt to start the network controller");
                return;
            }

            this.log.info("Ruby Network Controller Starting");

            sourcesNeedToStart = new LinkedList<>(this.networkEventSources);

            started = true;
        }

        for (NetworkEventSource source : sourcesNeedToStart) {
            source.setListener(this.myNetworkEventListener);
        }
    }

    @Override
    public void stop() {
        // TBD
    }

    @Override
    public void addNetworkEventSource(NetworkEventSource newEventSource) {
        boolean startSource;
        synchronized (this) {
            this.networkEventSources.add(newEventSource);
            startSource = this.started;
        }

        if (startSource) {
            newEventSource.setListener(this.myNetworkEventListener);
        }
    }

    /**
     * @param event
     */
    protected void processNetworkEvent(NetworkEvent event) {
        if (event instanceof AddConsumerEvent) {
            AddConsumerEvent addConsumerEvent = (AddConsumerEvent) event;

            Destination dest = addConsumerEvent.getJmsDestination();
            this.log.info("ADDING CONSUMER: brokerId={}; consumerId={}; destination={}", addConsumerEvent.getBrokerId(),
                    addConsumerEvent.getConsumerId(), dest);

            // TBD: process the consumer add, creating bridges as-needed to move messages to consumers from producers /
            // TBD: stores
            try {
                if (dest instanceof Queue) {
                    String queueName = ((Queue) dest).getQueueName();
                    this.addBridgesForQueue(addConsumerEvent.getBrokerId(), queueName);
                }
            } catch (JMSException jmsExc) {
                this.log.warn("unexpected JMS exception processing add consumer event", jmsExc);
            }
        } else if (event instanceof RemoveConsumerEvent) {
            RemoveConsumerEvent removeConsumerEvent = (RemoveConsumerEvent) event;

            this.log.info("REMOVING CONSUMER: brokerId={}; consumerId={}; destination={}",
                    removeConsumerEvent.getBrokerId(), removeConsumerEvent.getConsumerId(),
                    removeConsumerEvent.getJmsDestination());

            // TBD: process the consumer removal, shutting-down bridges / changing bridges as-needed to move messages
            // TBD: to consumers from producers / stores
        }
    }

    protected void addBridgesForQueue(String consumerBrokerId, String queueName) {
        BrokerInfo consumerBrokerInfo = this.oldBrokerRegistry.lookupBrokerById(consumerBrokerId);

        if (consumerBrokerInfo == null) {
            this.log.warn("lost a race condition? missing source broker on adding bridges: src-broker-id={}; " +
                    "queue-name={}", consumerBrokerId, queueName);
            return;
        }

        for (String brokerId : this.oldBrokerRegistry.getBrokerIds()) {
            if (brokerId.equals(consumerBrokerId)) {
                log.debug("skipping bridge between source broker and itself: brokerId={}", brokerId);
            } else {
                log.info("adding bridge for queue: source-broker-id={}; dest-broker-id={}; queue={}", consumerBrokerId,
                        brokerId, queueName);

                BrokerInfo anotherBrokerInfo = this.oldBrokerRegistry.lookupBrokerById(brokerId);

                if (anotherBrokerInfo != null) {
                    String bridgeId = "auto-bridge-" + brokerId + "-to-" + consumerBrokerId;
                    AmqBridgeSpec bridgeSpec = this.bridgeController.getBridgeSpecs().get(bridgeId);

                    try {
                        if (bridgeSpec != null) {
                            AmqBridgeSpec updBridgeSpec = bridgeSpec.copy();
                            updBridgeSpec.getQueueList().add(queueName);
                            // TBD: allow for real-time updates to bridges!
                            try {
                                this.bridgeController.stopBridge(bridgeId);
                            } catch (BridgeNotActiveException e) {
                                this.log.debug("attempt to stop bridge that was not active");
                            }
                            this.bridgeController.updateBridge(bridgeId, updBridgeSpec);
                            this.bridgeController.startBridge(bridgeId);
                        } else {
                            bridgeSpec = new AmqBridgeSpecImpl();
                            bridgeSpec.setId(bridgeId);
                            bridgeSpec.setSrcUrl(anotherBrokerInfo.getBrokerUrl());
                            bridgeSpec.setDestUrl(consumerBrokerInfo.getBrokerUrl());
                            bridgeSpec.getQueueList().add(queueName);

                            this.bridgeController.addBridge(bridgeSpec);
                            this.bridgeController.startBridge(bridgeId);
                        }
                    } catch (BridgeAlreadyExistsException alreadyExistsExc) {
                        this.log.warn("lost unexpected race on creating bridge", alreadyExistsExc);
                    } catch (AmqBridgeConfigurationException bridgeConfigExc) {
                        this.log.warn("unexpected bridge confguration excepton", bridgeConfigExc);
                    } catch (JMSException jmsExc) {
                        this.log.warn("unexpected JMS exception creating new bridge", jmsExc);
                    } catch (BridgeNotDefinedException bridgeDefMissingExc) {
                        this.log.warn("unexpected bridge definition missing: lost race condition?", bridgeDefMissingExc);
                    } catch (BridgeActiveException bridgeActiveExc) {
                        // TBD999: hmm...
                        this.log.warn("TBD: need to handle this somehow...", bridgeActiveExc);
                    } catch (BridgeAlreadyActiveException bridgeAlreadyActiveExc) {
                        this.log.warn("unexpected bridge already active", bridgeAlreadyActiveExc);
                    } catch (BridgeControllerNotActiveException bridgeControllerNotActiveExc) {
                        this.log.warn("bridge controller not yet active on starting new bridge",
                                bridgeControllerNotActiveExc);
                    }
                } else {
                    this.log.warn("lost a race condition? broker not found in registry: broker-id={}", brokerId);
                }
            }
        }
    }

    protected class MyNetworkEventListener implements NetworkEventListener {
        @Override
        public void onNetworkEvent(NetworkEvent event) {
            processNetworkEvent(event);
        }
    }
}
