package com.amlinv.activemq.monitor.jmx.connection.impl;

import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnection;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnectionFactory;
import org.jolokia.client.J4pClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by art on 4/1/15.
 */
public class JolokiaConnectionFactory implements MBeanAccessConnectionFactory {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JolokiaConnectionFactory.class);

    /**
     * Full URL for accessing Jolokia (e.g. http://localhost:8161/api/jolokia).
     */
    private final String jolokiaUrl;

    private Logger log = DEFAULT_LOGGER;

    public JolokiaConnectionFactory(String initJolokiaUrl) {
        this.jolokiaUrl = initJolokiaUrl;
    }

    @Override
    public MBeanAccessConnection createConnection() throws IOException {
        J4pClient client = new J4pClient(this.jolokiaUrl);
        JolokiaConnection connection = new JolokiaConnection(client);

        return connection;
    }

    @Override
    public String getTargetDescription() {
        return "jolokia:url=" + this.jolokiaUrl;
    }
}
