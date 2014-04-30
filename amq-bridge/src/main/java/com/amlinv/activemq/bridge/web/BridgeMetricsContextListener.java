package com.amlinv.activemq.bridge.web;

import com.amlinv.activemq.bridge.engine.AmqBridgeStatistics;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;

/**
 * Created by art on 4/28/14.
 */
public class BridgeMetricsContextListener extends MetricsServlet.ContextListener {
    private MetricRegistry registry;

    public BridgeMetricsContextListener () {
        super();

        this.registry = new MetricRegistry();

        // I dislike this, but can't find a way to inject the value through Spring and/or web.xml.
        AmqBridgeStatistics.setDefaultMetricRegistry(registry);
    }

    @Override
    protected MetricRegistry getMetricRegistry() {
        return registry;
    }

    public void setMetricRegistry (MetricRegistry newReg) {
        this.registry = newReg;
    }
}
