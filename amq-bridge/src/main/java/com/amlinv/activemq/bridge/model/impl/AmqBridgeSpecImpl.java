package com.amlinv.activemq.bridge.model.impl;

import com.amlinv.activemq.bridge.model.AmqBridgeSpec;
import com.amlinv.activemq.bridge.ctlr.events.AmqBridgeListener;
import org.codehaus.jackson.annotate.JsonAnySetter;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by art on 4/19/14.
 */
@XmlRootElement(name = "AmqBridgeSpec")
public class AmqBridgeSpecImpl implements AmqBridgeSpec {
    private String                          id;
    private String                          srcUrl;
    private String                          destUrl;
    private boolean                         runningInd = false;
    private final List<AmqBridgeListener>   listeners = new LinkedList<AmqBridgeListener>();
    private List<String>                    queueList = new LinkedList<String>();
    private List<String>                    topicList = new LinkedList<String>();

    @Override public String getId() {
        return id;
    }

    @Override public void setId(String id) {
        this.id = id;
    }

    @Override public String getSrcUrl() {
        return srcUrl;
    }

    @Override public void setSrcUrl(String srcUrl) {
        this.srcUrl = srcUrl;
    }

    @Override public String getDestUrl() {
        return destUrl;
    }

    @Override public void setDestUrl(String destUrl) {
        this.destUrl = destUrl;
    }

    @Override
    public List<String> getQueueList () {
        return  this.queueList;
    }

    @Override
    public void setQueueList (List<String> list) {
        this.queueList = list;
    }

    @Override
    public List<String> getTopicList () {
        return  this.topicList;
    }

    @Override
    public void setTopicList (List<String> list) {
        this.topicList = list;
    }

    @JsonAnySetter
    public void setAnyMisc (String name, Object value) {
        // Ignore extra fields.
    }

    @Override
    public AmqBridgeSpec copy () {
        AmqBridgeSpec dup = new AmqBridgeSpecImpl();
        dup.setDestUrl(this.destUrl);
        dup.setSrcUrl(this.srcUrl);
        dup.setId(this.id);
        dup.setQueueList(new LinkedList<>(this.queueList));
        dup.setTopicList(new LinkedList<>(this.topicList));

        return  dup;
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
        return this.runningInd;
    }
}
