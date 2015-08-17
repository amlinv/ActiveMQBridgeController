package com.amlinv.jmxutil.connection;

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
    /**
     * Query the specified attributes of the mbean with the given object name.  May be called multiple times
     * concurrently.
     *
     * @param objectName object name of the mbean to query.
     * @param attributeNames names of the attributes to retrieve.
     * @return list of attribute values.
     * @throws InstanceNotFoundException if the mbean with the given object name does not exist.
     * @throws IOException
     * @throws ReflectionException
     */
    List<Attribute> getAttributes(ObjectName objectName, String... attributeNames) throws InstanceNotFoundException,
            IOException, ReflectionException;

    /**
     * Query object names of mbeans given an object name pattern and, optionally, a query expression.
     *
     * @param pattern object name pattern to query.
     * @param query optional query expression.
     * @return set of object names matching the given pattern and query expression.
     * @throws IOException
     * @throws MalformedObjectNameException
     */
    Set<ObjectName> queryNames(ObjectName pattern, QueryExp query) throws IOException, MalformedObjectNameException;

    /**
     * Close the connection.
     *
     * @throws IOException
     */
    void close() throws IOException;
}
