package com.amlinv.jmxutil.connection.impl;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pBulkRemoteException;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.jolokia.client.request.J4pSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by art on 5/7/15.
 */
public class JolokiaConnection implements MBeanBatchCapableAccessConnection {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JolokiaConnection.class);

    private Logger log = DEFAULT_LOGGER;

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
    public Map<ObjectName, List<Attribute>> batchQueryAttributes(Map<ObjectName, List<String>> objectAttNames)
            throws IOException, ReflectionException, MalformedObjectNameException {

        Map<ObjectName, List<Attribute>> result;
        List<J4pReadRequest> requests = new LinkedList<>();

        for ( ObjectName oneObjectName : objectAttNames.keySet() ) {
            List<String> attributeNames = objectAttNames.get(oneObjectName);
            String[] attributeNameArray = new String[attributeNames.size()];
            attributeNameArray = attributeNames.toArray(attributeNameArray);

            J4pReadRequest oneReadRequest = new J4pReadRequest(oneObjectName, attributeNameArray);
            requests.add(oneReadRequest);
        }

        try {
            List<J4pReadResponse> responses = this.jolokiaClient.execute(requests);
            result = this.copyOutBatchAttributes(responses, objectAttNames);
        } catch (J4pBulkRemoteException j4pBulkRemoteExc ) {
            //
            // May have a partial result; copy out what we can.
            //
            result = this.copyOutBatchAttributes(j4pBulkRemoteExc.getResponses(), objectAttNames);
        } catch (J4pException jolokiaExc) {
            // TODO: consider finer analysis of the exception
            throw new IOException("jolokia request failure", jolokiaExc);
        }

        return result;
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

    protected Map<ObjectName, List<Attribute>> copyOutBatchAttributes (List responses,
                                                                       Map<ObjectName, List<String>> objectAttNames)
            throws MalformedObjectNameException {

        Map<ObjectName, List<Attribute>> result = new HashMap<>();

        for ( Object oneResponse : responses ) {
            if ( oneResponse instanceof J4pReadResponse ) {
                J4pReadResponse j4pReadResponse = (J4pReadResponse) oneResponse;

                for (ObjectName oneObjectName : j4pReadResponse.getObjectNames()) {
                    List<Attribute> values = new LinkedList<>();
                    List<String> attNames = objectAttNames.get(oneObjectName);

                    for (String oneAttributeName : attNames) {
                        Object value = j4pReadResponse.getValue(oneObjectName, oneAttributeName);
                        Attribute att = new Attribute(oneAttributeName, value);
                        values.add(att);
                    }

                    result.put(oneObjectName, values);
                }
            } else if ( oneResponse instanceof Exception ) {
                this.log.info("error on element of a bulk query", (Exception) oneResponse);
            } else if ( oneResponse == null ) {
                this.log.info("unexpected null response on element of a bulk query");
            } else {
                if ( this.log.isDebugEnabled() ) {
                    this.log.info("unexpected response on element of a bulk query: class={}; object={}",
                            oneResponse.getClass().getName(), oneResponse);
                } else {
                    this.log.info("unexpected response on element of a bulk query: class={}",
                            oneResponse.getClass().getName());
                }
            }
        }

        return result;
    }
}
