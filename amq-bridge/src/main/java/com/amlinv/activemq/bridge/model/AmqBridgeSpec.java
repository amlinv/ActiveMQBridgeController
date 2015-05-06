package com.amlinv.activemq.bridge.model;

import com.amlinv.activemq.bridge.engine.AmqBridge;

import java.util.List;

/**
 * Specification for a bridge between two ActiveMQ instances.
 *
 * Created by art on 4/20/14.
 */
public interface AmqBridgeSpec {
    String getId();

    void setId(String id);

    String getSrcUrl();

    void setSrcUrl(String srcUrl);

    String getDestUrl();

    void setDestUrl(String destUrl);

    List<String>    getQueueList();

    void    setQueueList(List<String> queueList);

    List<String>    getTopicList();

    void    setTopicList(List<String> topicList);

    AmqBridgeSpec copy();
}
