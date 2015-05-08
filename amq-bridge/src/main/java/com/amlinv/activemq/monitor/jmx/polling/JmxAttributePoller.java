package com.amlinv.activemq.monitor.jmx.polling;

import com.amlinv.activemq.monitor.jmx.annotation.MBeanAnnotationUtil;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnection;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnectionFactory;
import com.amlinv.activemq.monitor.model.MBeanLocationParameterSource;
import com.amlinv.logging.RepeatLogMessageSuppressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by art on 3/31/15.
 */
public class JmxAttributePoller {
    private static final Logger log = LoggerFactory.getLogger(JmxAttributePoller.class);

    private final Pattern replaceParamPattern = Pattern.compile("\\$\\{(?<paramName>[^}]*)\\}");

    private final List<Object> polledObjects;

    private MBeanAccessConnectionFactory mBeanAccessConnectionFactory;
    private MBeanAccessConnection mBeanAccessConnection;

    private RepeatLogMessageSuppressor logInstanceNotFoundThrottle = new RepeatLogMessageSuppressor();
    private RepeatLogMessageSuppressor logNoAttributeThrottle = new RepeatLogMessageSuppressor();

    private boolean shutdownInd = false;
    private boolean pollActiveInd = false;

    public JmxAttributePoller(List<Object> polledObjects) {
        this.polledObjects = polledObjects;
    }

    public MBeanAccessConnectionFactory getmBeanAccessConnectionFactory() {
        return mBeanAccessConnectionFactory;
    }

    public void setmBeanAccessConnectionFactory(MBeanAccessConnectionFactory mBeanAccessConnectionFactory) {
        this.mBeanAccessConnectionFactory = mBeanAccessConnectionFactory;
    }

    public List<Object> getPolledObjects() {
        return Collections.unmodifiableList(polledObjects);
    }

    public void poll () throws IOException {
        synchronized ( this ) {
            // Make sure not to check and create a connection if shutting down.
            if ( shutdownInd ) {
                return;
            }

            // Atomically indicate polling is active now so a caller can determine with certainty whether polling is
            //  completely shutdown.
            pollActiveInd = true;
        }

        try {
            this.checkConnection();

            for ( Object onePolledObject : this.polledObjects ) {
                // Stop as soon as possible if shutting down.
                if ( shutdownInd ) {
                    return;
                }

                try {
                    this.pollOneObject(onePolledObject);
                } catch ( ReflectionException | InvocationTargetException | IllegalAccessException exc ) {
                    log.warn("failed to poll object: class={}", onePolledObject.getClass().getName());
                } catch (MalformedObjectNameException monExc) {
                    log.warn("invalid object name", monExc);
                }
            }
        } catch ( IOException ioExc ) {
            this.safeClose(this.mBeanAccessConnection);
            this.mBeanAccessConnection = null;

            throw ioExc;
        } finally {
            synchronized ( this ) {
                pollActiveInd = false;
                this.notifyAll();
            }
        }
    }

    public void shutdown () {
        this.shutdownInd = true;
    }

    public void waitUntilShutdown () throws InterruptedException {
        synchronized ( this ) {
            // Wait until shutdown is initiated.
            while ( ! this.shutdownInd ) {
                this.wait();
            }

            // Wait until any active polling stops.
            while ( pollActiveInd ) {
                this.wait();
            }
        }
    }

    protected void  checkConnection () throws IOException {
        if ( this.mBeanAccessConnection == null ) {
            this.mBeanAccessConnection = this.mBeanAccessConnectionFactory.createConnection();
        }
    }

    protected void pollOneObject (Object obj)
            throws MalformedObjectNameException, IOException, ReflectionException, InvocationTargetException,
            IllegalAccessException {

        //
        // Extract the mbean info from the object (TBD: cache this information ahead of time)
        //
        String onamePattern = MBeanAnnotationUtil.getLocationONamePattern(obj);
        if (onamePattern == null) {
            log.warn("ignoring attempt to poll object that has no MBeanLocation");
            return;
        }

        Map<String, Method> attributeSetters = MBeanAnnotationUtil.getAttributes(obj);

        if (attributeSetters.size() > 0) {
            String onameString;
            if ( obj instanceof MBeanLocationParameterSource ) {
                // TBD: cache this
                onameString = this.replaceObjectNameParameters(onamePattern, (MBeanLocationParameterSource) obj);
            } else {
                onameString = onamePattern;
            }

            ObjectName oname = new ObjectName(onameString);

            try {
                String[] attributeNames = new String[attributeSetters.size()];

                int cur = 0;
                for (String oneName : attributeSetters.keySet()) {
                    attributeNames[cur] = oneName;
                    cur++;
                }

                List<Attribute> attributeValues = this.mBeanAccessConnection.getAttributes(oname, attributeNames);

                this.copyOutAttributes(obj, attributeValues, attributeSetters);
            } catch (InstanceNotFoundException infExc) {
                this.logInstanceNotFoundThrottle.info(log, "instance not found on polling object: oname={}", oname,
                        infExc);
            }
        } else {
            this.logNoAttributeThrottle.warn(log,
                    "ignoring attempt to poll MBean object with no attributes: onamePattern={}", onamePattern);
        }
    }

    protected void copyOutAttributes (Object target, List<Attribute> jmxAttributeValues,
                                      Map<String, Method> attributeSetters)
            throws InvocationTargetException, IllegalAccessException {

        for ( Attribute oneAttribute : jmxAttributeValues ) {
            String attributeName = oneAttribute.getName();

            Method setter = attributeSetters.get(attributeName);
            Object value = oneAttribute.getValue();

            setter.invoke(target, value);
        }
    }

    protected String replaceObjectNameParameters (String pattern, MBeanLocationParameterSource parameterSource) {
        Matcher matcher = replaceParamPattern.matcher(pattern);
        StringBuffer result = new StringBuffer();

        while ( matcher.find() ) {
            String name = matcher.group("paramName");
            String value = parameterSource.getParameter(name);

            if ( value != null ) {
                matcher.appendReplacement(result, value);
            } else {
                matcher.appendReplacement(result, matcher.group());
            }
        }

        matcher.appendTail(result);

        return  result.toString();
    }

    protected void  safeClose (MBeanAccessConnection mBeanAccessConnector) {
        try {
            if ( mBeanAccessConnector != null ) {
                mBeanAccessConnector.close();
            }
        } catch ( IOException ioExc ) {
            log.warn("exception on shutdown of jmx connection to {}",
                    this.mBeanAccessConnectionFactory.getTargetDescription(), ioExc);
        }
    }
}
