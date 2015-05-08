package com.amlinv.activemq.monitor.jmx.connection.impl;

import com.amlinv.activemq.monitor.jmx.connection.JMXConnectionSource;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnection;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by art on 4/1/15.
 */
public class JMXRemoteUrlConnectionFactory implements MBeanAccessConnectionFactory {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JMXRemoteUrlConnectionFactory.class);

    private final JMXServiceURL url;
    private Logger log = DEFAULT_LOGGER;

    public JMXRemoteUrlConnectionFactory(JMXServiceURL url) {
        this.url = url;
    }

    public JMXRemoteUrlConnectionFactory(String urlString) throws MalformedURLException {
        this(new JMXServiceURL(urlString));
    }

    @Override
    public MBeanAccessConnection createConnection() throws IOException {
        JMXConnector jmxConnector = JMXConnectorFactory.connect(this.url);

        boolean success = false;
        try {
            JMXMBeanConnection result = new JMXMBeanConnection(jmxConnector);
            success = true;

            return  result;
        } finally {
            if ( ! success ) {
                try {
                    jmxConnector.close();
                } catch ( IOException ioExc ) {
                    this.log.info("IO exception closing jmx connector after mbean connection failure", ioExc);
                }
            }
        }
    }

    @Override
    public String getTargetDescription() {
        return "jmx:url=" + this.url;
    }
}
