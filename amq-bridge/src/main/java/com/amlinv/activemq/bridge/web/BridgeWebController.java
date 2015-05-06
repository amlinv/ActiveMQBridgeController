package com.amlinv.activemq.bridge.web;

import com.amlinv.activemq.bridge.ctlr.AmqBridgeController;
import com.amlinv.activemq.bridge.ctlr.events.AmqBridgeEvent;
import com.amlinv.activemq.bridge.ctlr.events.AmqBridgeListener;
import com.amlinv.activemq.bridge.engine.AmqBridgeStatistics;
import com.amlinv.activemq.bridge.model.AmqBridgeSpec;

import javax.ws.rs.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import com.amlinv.activemq.bridge.model.impl.AmqBridgeSpecImpl;
import org.codehaus.jackson.io.JsonStringEncoder;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for a bridge which watches and makes requested changes to state.
 *
 * Created by art on 4/19/14.
 */
@Path("/bridges")
public class BridgeWebController {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeWebController.class);

    private final AmqBridgeController       engine;

    BridgeWebController(AmqBridgeController initEngine) {
        this.engine = initEngine;
        this.engine.addListener(new AmqBridgeListener() {
            @Override
            public void onEvent(AmqBridgeEvent bridgeEvent) {
                fireEvent("bridgeEvent", objectToJson(bridgeEvent, "null"));
            }
        });
    }

    @GET
    @Consumes("application/json")
    public List<AmqBridgeSpec> listBridges() {
        LOG.debug("listBridges");

        return  new LinkedList<AmqBridgeSpec>(this.engine.getBridgeSpecs().values());
    }

    @GET
    @Path("/stats")
    @Consumes("application/json")
    public List<AmqBridgeStatistics> getAllBridgeStats() {
        LOG.debug("getAllBridgeStats");

        return  this.engine.getBridgeStats();
    }

    @PUT
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public AmqBridgeSpec    addBridge (@PathParam("id") String id, AmqBridgeSpecImpl bridgeInfo) throws Exception {
        LOG.debug("addBridge() ID={}", id);

        //
        // Override the ID from the bridge structure itself.  In this way, structures can easily be copied.
        //
        bridgeInfo.setId(id);
        this.engine.addBridge(bridgeInfo);

        fireEvent("add", objectToJson(bridgeInfo, "null"));

        return  bridgeInfo;
    }

    @POST
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public AmqBridgeSpec    updateBridge (@PathParam("id") String id, AmqBridgeSpecImpl bridgeInfo) throws Exception {
        LOG.debug("updateBridge() ID={}", id);

        this.engine.updateBridge(id, bridgeInfo);

        fireEvent("update", objectToJson(bridgeInfo, "null"));

        return  bridgeInfo;
    }

    @GET
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public AmqBridgeSpec getBridge (@PathParam("id") String id) throws Exception {
        LOG.debug("getBridge ID={}", id);

        return  this.engine.getBridgeSpecs().get(id);
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    public void deleteBridge (@PathParam("id") String id) throws Exception {
        LOG.debug("deleteBridge ID={}", id);

        this.engine.deleteBridge(id);

        String idString = new String(JsonStringEncoder.getInstance().quoteAsString(id));
        fireEvent("remove", "{ \"id\":\"" + idString + "\"}");
    }

    @GET
    @Path("/{id}/start")
    @Consumes("application/json")
    @Produces("application/json")
    public String   startBridge (@PathParam("id") String id) throws Exception {
        LOG.debug("startBridge ID={}", id);

        engine.startBridge(id);
        return  "starting";
    }

    @GET
    @Path("/{id}/stop")
    @Consumes("application/json")
    @Produces("application/json")
    public String   stopBridge (@PathParam("id") String id) throws Exception {
        LOG.debug("stopBridge ID={}", id);

        this.engine.stopBridge(id);

        return  "stopped";
    }


    /**
     * TBD: for testing purposes only
     */
    @GET
    @Path("/testCreateBridges")
    @Consumes("application/json")
    @Produces("application/json")
    public String testCreateBridges () throws Exception {
        LOG.debug("testCreateBridges");

        AmqBridgeSpecImpl newBridge;
        for ( String[] props :
            new String[][]
            {
                new String[] { "invPhxToAsh1", "tcp://inv.amq.phx:61616", "tcp://inv.amq.ash:61616" },
                new String[] { "invPhxToAsh2", "tcp://inv.amq.phx:61613", "tcp://inv.amq.ash:61613" },
                new String[] { "invAshToPhx1", "tcp://inv.amq.ash:61616", "tcp://inv.amq.phx:61616" },
                new String[] { "invAshToPhx2", "tcp://inv.amq.ash:61613", "tcp://inv.amq.phx:61613" },
            }
        ) {
            newBridge = new AmqBridgeSpecImpl();
            newBridge.setId(props[0]);
            newBridge.setSrcUrl(props[1]);
            newBridge.setDestUrl(props[2]);

            this.addBridge(props[0], newBridge);
        }

        return  "{ 'result' : 'success' }";
    }

    private void fireEvent(String action, String json) {
        AmqBridgeWebsocket.fireBridgeEvent(action, json);
    }

    private String objectToJson (Object obj, String fallback) {
        ObjectMapper mapper = new ObjectMapper();
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        try {
            mapper.writeValue(capture, obj);
            return  capture.toString();
        } catch (IOException exc) {
            LOG.warn("failed to convert bridge to json", exc);
        }

        return  fallback;
    }
}
