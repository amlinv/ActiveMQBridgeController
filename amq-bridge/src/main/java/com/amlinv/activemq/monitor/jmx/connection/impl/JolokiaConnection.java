package com.amlinv.activemq.monitor.jmx.connection.impl;

import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnection;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.jolokia.client.request.J4pRequest;
import org.jolokia.client.request.J4pSearchRequest;
import org.jolokia.client.request.J4pSearchResponse;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by art on 5/7/15.
 */
public class JolokiaConnection implements MBeanAccessConnection {
    private final J4pClient jolokiaClient;

    public JolokiaConnection(J4pClient initJolokiaClient) {
        this.jolokiaClient = initJolokiaClient;
    }

    @Override
    public List<Attribute> getAttributes(ObjectName objectName, String... attributeNames)
            throws InstanceNotFoundException, IOException, ReflectionException {

        try {
            J4pReadRequest request = new J4pReadRequest(objectName, attributeNames);
            J4pReadResponse response = this.jolokiaClient.execute(request);

            List<Attribute> result = new LinkedList<Attribute>();
            for ( String oneAtt : attributeNames ) {
                Attribute attribute = new Attribute(oneAtt, response.getValue(oneAtt));
                result.add(attribute);
            }

            return  result;
        } catch (J4pException jolokiaExc) {
            // TODO: consider finer analysis of the exception
            throw new IOException("jolokia request failure", jolokiaExc);
        }
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName pattern, QueryExp query) throws IOException, MalformedObjectNameException {
        try {
            J4pSearchRequest searchRequest = new J4pSearchRequest(pattern.toString());
            J4pSearchResponse response = this.jolokiaClient.execute(searchRequest);

            Set<ObjectName> result = new HashSet<>(response.getObjectNames());

            return result;
        } catch (J4pException jolokiaExc) {
            throw new IOException("jolokia request failure", jolokiaExc);
        }
    }


    @Override
    public void close() throws IOException {
    }
}
