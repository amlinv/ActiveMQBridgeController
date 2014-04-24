package com.amlinv.activemq.bridge.model;

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
}
