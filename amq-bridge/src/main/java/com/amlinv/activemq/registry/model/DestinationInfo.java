package com.amlinv.activemq.registry.model;

/**
 * Created by art on 5/2/15.
 */
public class DestinationInfo {
    private final String name;

    public DestinationInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
