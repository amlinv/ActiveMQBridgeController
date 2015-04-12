package com.amlinv.activemq.monitor.jmx.polling;

import com.amlinv.activemq.monitor.jmx.connection.JMXConnectionSource;
import com.amlinv.activemq.monitor.jmx.connection.impl.JMXJvmIdConnectionSource;
import com.amlinv.activemq.monitor.jmx.connection.impl.JMXRemoteUrlConnectionSource;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.net.MalformedURLException;
import java.util.Set;

/**
 * Created by art on 3/31/15.
 */
public class JmxActiveMQUtil {
    public static final String JMX_URL_PREFIX = "service:jmx:rmi:///jndi/rmi://";
    public static final String JMX_URL_SUFFIX = "/jmxrmi";
    public static final String AMQ_BROKER_QUERY = "org.apache.activemq:type=Broker,brokerName=*";
    public static final String AMQ_BROKER_QUEUE_QUERY =
            "org.apache.activemq:type=Broker,brokerName=%s,destinationType=Queue,destinationName=*";

    public static final String AMQ_BROKER_NAME_KEY = "brokerName";
    public static final String AMQ_QUEUE_NAME_KEY = "destinationName";

    public static String    formatJmxUrl (String hostname, int port) {
        StringBuilder result = new StringBuilder();

        result.append(JMX_URL_PREFIX);
        result.append(hostname);
        result.append(":");
        result.append(Integer.toString(port));
        result.append(JMX_URL_SUFFIX);

        return  result.toString();
    }

    /**
     * Supports the following formats:
     * <ul>
     *     <li>Full JMX url starting with "service:" (e.g. service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi)</li>
     *     <li>JVM ID starting with "jvmId=" or "pid="</li>
     *     <li>Broker hostname and port separated by a colon (e.g. localhost:1099)</li>
     * </ul>
     * @param location
     * @return
     */
    public static JMXConnectionSource getLocationConnectionSource (String location) throws Exception {
        JMXConnectionSource result;
        if ( location.startsWith("service:") ) {
            result = new JMXRemoteUrlConnectionSource(location);
        } else if ( location.startsWith("jvmId=") || location.startsWith("pid=") ) {
            String id = location.replaceFirst("[^=]*=", "");
            result = new JMXJvmIdConnectionSource(id);
        } else {
            String parts[] = location.split(":");
            if ( parts.length == 2 ) {
                String fullUrl = formatJmxUrl(parts[0], Integer.valueOf(parts[1]));
                result = new JMXRemoteUrlConnectionSource(fullUrl);
            } else {
                throw new Exception("invalid location: " + location);
            }
        }

        return  result;
    }

    public static String[] queryBrokerNames (String location) throws Exception {
        String[] names;

        Set<ObjectName> matches;
        matches = execLocationQuery(location, new ObjectName(AMQ_BROKER_QUERY));

        names = new String[matches.size()];
        int cur = 0;

        for ( ObjectName oneBrokerMBeanName : matches ) {
            names[cur] = oneBrokerMBeanName.getKeyProperty(AMQ_BROKER_NAME_KEY);
            cur++;
        }

        return  names;
    }

    public static String[] queryQueueNames (String location, String brokerName, String queueNamePattern)
            throws Exception {

        String[] names = null;

        Set<ObjectName> matches;
        String pattern = String.format(AMQ_BROKER_QUEUE_QUERY, brokerName);
        matches = execLocationQuery(location, new ObjectName(pattern));

        names = new String[matches.size()];
        int cur = 0;

        for ( ObjectName oneQueueMBeanName : matches ) {
            names[cur] = oneQueueMBeanName.getKeyProperty(AMQ_QUEUE_NAME_KEY);
            cur++;
        }

        return  names;
    }

    protected static Set<ObjectName> execLocationQuery (String location, ObjectName pattern) throws Exception {
        JMXConnectionSource connectionSource = getLocationConnectionSource(location);
        JMXConnector connector = connectionSource.createConnection();
        MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();

        Set<ObjectName> matches;
        matches = mBeanServerConnection.queryNames(pattern, null);

        return  matches;
    }
}
