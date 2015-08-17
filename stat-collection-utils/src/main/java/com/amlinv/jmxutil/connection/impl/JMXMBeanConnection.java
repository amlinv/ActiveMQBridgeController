package com.amlinv.jmxutil.connection.impl;

import com.amlinv.jmxutil.connection.MBeanAccessConnection;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by art on 5/7/15.
 */
public class JMXMBeanConnection implements MBeanAccessConnection {
    private final JMXConnector jmxConnector;
    private final MBeanServerConnection mBeanServerConnection;

    public JMXMBeanConnection(JMXConnector jmxConnector) throws IOException {
        this.jmxConnector = jmxConnector;
        this.mBeanServerConnection = this.jmxConnector.getMBeanServerConnection();
    }

    @Override
    public List<Attribute> getAttributes(ObjectName objectName, String... attributeNames)
            throws InstanceNotFoundException, IOException, ReflectionException {

        return this.mBeanServerConnection.getAttributes(objectName, attributeNames).asList();
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName pattern, QueryExp query) throws IOException {
        return this.mBeanServerConnection.queryNames(pattern, query);
    }

    @Override
    public void close() throws IOException {
        this.jmxConnector.close();
    }
}
