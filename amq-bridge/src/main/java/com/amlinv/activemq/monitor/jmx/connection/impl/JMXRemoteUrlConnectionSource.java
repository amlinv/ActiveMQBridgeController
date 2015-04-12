package com.amlinv.activemq.monitor.jmx.connection.impl;

import com.amlinv.activemq.monitor.jmx.connection.JMXConnectionSource;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by art on 4/1/15.
 */
public class JMXRemoteUrlConnectionSource implements JMXConnectionSource {
    private final JMXServiceURL url;

    public JMXRemoteUrlConnectionSource(JMXServiceURL url) {
        this.url = url;
    }

    public JMXRemoteUrlConnectionSource(String urlString) throws MalformedURLException {
        this(new JMXServiceURL(urlString));
    }

    @Override
    public JMXConnector createConnection() throws IOException {
        return JMXConnectorFactory.connect(this.url);
    }

    @Override
    public String getTargetDescription() {
        return "url=" + this.url;
    }
}
