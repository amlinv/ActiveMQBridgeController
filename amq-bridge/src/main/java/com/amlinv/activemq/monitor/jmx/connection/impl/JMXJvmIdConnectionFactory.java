package com.amlinv.activemq.monitor.jmx.connection.impl;

import com.amlinv.activemq.monitor.jmx.connection.JMXConnectionSource;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnection;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnectionFactory;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;

/**
 *
 * http://stackoverflow.com/questions/5552960/how-to-connect-to-a-java-program-on-localhost-jvm-using-jmx
 * http://docs.oracle.com/javase/8/docs/technotes/guides/management/agent.html
 * Created by art on 4/1/15.
 */
public class JMXJvmIdConnectionFactory implements MBeanAccessConnectionFactory {
    private static final Logger log = LoggerFactory.getLogger(JMXJvmIdConnectionFactory.class);
    private static final String COM_SUN_LOCAL_CONNECTOR_ADDRESS_PROPERTY =
            "com.sun.management.jmxremote.localConnectorAddress";

    private final String jvmId;

    public JMXJvmIdConnectionFactory(String jvmId) {
        this.jvmId = jvmId;
    }

    @Override
    public MBeanAccessConnection createConnection() throws IOException {
        JMXMBeanConnection result = null;

        try {
            VirtualMachine vm = VirtualMachine.attach(this.jvmId);
            String url = vm.getAgentProperties().getProperty(COM_SUN_LOCAL_CONNECTOR_ADDRESS_PROPERTY);

            if ( url == null ) {
                String javaHome = vm.getSystemProperties().getProperty("java.home");
                String agent = javaHome + File.separator + "lib" + File.separator + "management-agent.jar";
                vm.loadAgent(agent);

                url = vm.getAgentProperties().getProperty(COM_SUN_LOCAL_CONNECTOR_ADDRESS_PROPERTY);
            }

            if ( url != null ) {
                JMXServiceURL jmxUrl = new JMXServiceURL(url);
                JMXConnector connector = JMXConnectorFactory.connect(jmxUrl);
                result = new JMXMBeanConnection(connector);
            } else {
                log.warn("failed to find the local connection url for jvm: jvmId={}", this.jvmId);
            }
        } catch ( AgentInitializationException | AgentLoadException | AttachNotSupportedException exc ) {
            log.warn("failed to connect to jvm: jvmId={}", this.jvmId, exc);
        }

        return result;
    }

    @Override
    public String getTargetDescription() {
        return "jvmId=" + this.jvmId;
    }
}
