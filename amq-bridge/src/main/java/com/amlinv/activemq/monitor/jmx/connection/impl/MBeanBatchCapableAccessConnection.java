package com.amlinv.activemq.monitor.jmx.connection.impl;

import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnection;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by art on 5/7/15.
 */
public interface MBeanBatchCapableAccessConnection extends MBeanAccessConnection {
    /**
     * Execute a batch query of the attributes for multiple object names.  Each object name may query its own set of
     * attributes.
     *
     * @param objectAttNames set of object names for which to query attributes mapped to the list of attributes for
     *                       each object name.
     * @return map of attribute values for each object name; note an object name will be missing from the map if it
     * resulted in an error.
     * @throws java.io.IOException
     * @throws javax.management.ReflectionException
     */
    Map<ObjectName, List<Attribute>> batchQueryAttributes(Map<ObjectName, List<String>> objectAttNames)
            throws IOException, ReflectionException, MalformedObjectNameException;
}