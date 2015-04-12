package com.amlinv.activemq.monitor.jmx.connection.impl;

import com.amlinv.activemq.monitor.jmx.connection.JMXConnectionSource;
import com.sun.tools.attach.*;
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
public class JMXJvmIdConnectionSource implements JMXConnectionSource {
    private static final Logger log = LoggerFactory.getLogger(JMXJvmIdConnectionSource.class);
    private static final String COM_SUN_LOCAL_CONNECTOR_ADDRESS_PROPERTY =
            "com.sun.management.jmxremote.localConnectorAddress";

    private final String jvmId;

    public JMXJvmIdConnectionSource(String jvmId) {
        this.jvmId = jvmId;
    }

    @Override
    public JMXConnector createConnection() throws IOException {
        JMXConnector result = null;

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
                result = JMXConnectorFactory.connect(jmxUrl);
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
