package com.amlinv.activemq.monitor.jmx.connection;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by art on 5/7/15.
 */
public interface MBeanAccessConnection {
    List<Attribute> getAttributes(ObjectName objectName, String... attributeNames) throws InstanceNotFoundException,
            IOException, ReflectionException;

    Set<ObjectName> queryNames(ObjectName pattern, QueryExp query) throws IOException, MalformedObjectNameException;

    void close() throws IOException;
}
