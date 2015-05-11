package com.amlinv.activemq.monitor.jmx.polling;

import com.amlinv.activemq.monitor.jmx.annotation.MBeanAnnotationUtil;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnection;
import com.amlinv.activemq.monitor.jmx.connection.MBeanAccessConnectionFactory;
import com.amlinv.activemq.monitor.jmx.connection.impl.MBeanBatchCapableAccessConnection;
import com.amlinv.activemq.monitor.model.MBeanLocationParameterSource;
import com.amlinv.logging.RepeatLogMessageSuppressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by art on 3/31/15.
 */
public class JmxAttributePoller {
    private static final Logger log = LoggerFactory.getLogger(JmxAttributePoller.class);

    private static final int DEFAULT_THREAD_POOL_CORE_COUNT = 20;
    private static final int DEFAULT_THREAD_POOL_MAX_COUNT = Integer.MAX_VALUE; // Unbounded queue, so this is meaningless
    private static final int DEFAULT_THREAD_POOL_KEEP_ALIVE_SEC = 60;

    private final Pattern replaceParamPattern = Pattern.compile("\\$\\{(?<paramName>[^}]*)\\}");

    private final List<Object> polledObjects;

    private MBeanAccessConnectionFactory mBeanAccessConnectionFactory;
    private MBeanAccessConnection mBeanAccessConnection;

    private RepeatLogMessageSuppressor logInstanceNotFoundThrottle = new RepeatLogMessageSuppressor();
    private RepeatLogMessageSuppressor logNoAttributeThrottle = new RepeatLogMessageSuppressor();

    private boolean shutdownInd = false;
    private boolean pollActiveInd = false;
    private ThreadPoolExecutor threadPoolExecutor;

    public JmxAttributePoller(List<Object> polledObjects) {
        this.polledObjects = polledObjects;
    }

    public MBeanAccessConnectionFactory getmBeanAccessConnectionFactory() {
        return mBeanAccessConnectionFactory;
    }

    public void setmBeanAccessConnectionFactory(MBeanAccessConnectionFactory mBeanAccessConnectionFactory) {
        this.mBeanAccessConnectionFactory = mBeanAccessConnectionFactory;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
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

            if ( this.threadPoolExecutor == null ) {
                this.threadPoolExecutor = createThreadPoolExecutor();
            }

            // Atomically indicate polling is active now so a caller can determine with certainty whether polling is
            //  completely shutdown.
            pollActiveInd = true;
        }

        try {
            this.checkConnection();

            if ( this.mBeanAccessConnection instanceof MBeanBatchCapableAccessConnection ) {
                this.pollBatch();
            } else {
                this.pollIndividually();
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

    protected boolean pollIndividually() {
        List<Future<Void>> calls = new LinkedList<>();
        for ( final Object onePolledObject : this.polledObjects ) {
            // Stop as soon as possible if shutting down.
            if ( shutdownInd ) {
                return true;
            }

            Future<Void> future =
                this.threadPoolExecutor.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        pollOneObject(onePolledObject);
                        return  null;
                    }
                });

            calls.add(future);
        }

        for ( Future<Void> oneFuture : calls ) {
            try {
                oneFuture.get();
            } catch (InterruptedException intExc) {
                log.info("interrupted while polling object");
            } catch (ExecutionException execExc) {
                log.warn("failed to poll object", execExc);
            }
        }
        return false;
    }

    protected void pollBatch () throws IOException {
        Map<ObjectName, List<String>> objectAttributes = new HashMap<>();
        Map<ObjectName, ObjectQueryInfo> objectQueryInfo = new HashMap<>();

        //
        // Collect the query details for all of the polled objects.
        //
        for (final Object onePolledObject : this.polledObjects) {
            // Stop as soon as possible if shutting down.
            if (shutdownInd) {
                return;
            }

            ObjectQueryInfo queryInfo = null;
            try {
                queryInfo = this.prepareObjectQuery(onePolledObject);

                if (queryInfo != null) {
                    objectAttributes.put(queryInfo.getObjectName(),
                            new LinkedList<String>(queryInfo.getAttributeNames()));
                    objectQueryInfo.put(queryInfo.getObjectName(), queryInfo);
                }
            } catch (MalformedObjectNameException malformedObjectNameExc) {
                this.log.info("invalid object name in query; skipping", malformedObjectNameExc);
            }
        }

        //
        // Poll them all in one batch now, if anything remains.
        //
        if (objectAttributes.size() > 0) {
            MBeanBatchCapableAccessConnection batchApi = (MBeanBatchCapableAccessConnection) this.mBeanAccessConnection;
            try {
                Map<ObjectName, List<Attribute>> objectAttValues = batchApi.batchQueryAttributes(objectAttributes);

                this.copyOutBatchAttributes(objectAttValues, objectQueryInfo);
            } catch (ReflectionException reflectionExc) {
                this.log.info("unexpected reflection exception during batch poll", reflectionExc);
            } catch (MalformedObjectNameException malformedObjectNameExc) {
                this.log.info("unexpected malformed object name during batch poll", malformedObjectNameExc);
            }
        } else {
            log.debug("nothing to poll after preparing {} objects", this.polledObjects.size());
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

    /**
     * Create a thread pool executor that will be used to execute individual queries.
     *
     * @return new thread pool executor.
     */
    protected ThreadPoolExecutor createThreadPoolExecutor () {
        ThreadFactory threadFactory = new ThreadFactory() {
            private AtomicLong threadCounter = new AtomicLong(0);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable);

                result.setName("jmx-attribute-poller-thread-" + this.threadCounter.incrementAndGet());

                return result;
            }
        };

        ThreadPoolExecutor result = new ThreadPoolExecutor(
                DEFAULT_THREAD_POOL_CORE_COUNT,
                DEFAULT_THREAD_POOL_MAX_COUNT,
                DEFAULT_THREAD_POOL_KEEP_ALIVE_SEC,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>(),
                threadFactory);

        return  result;
    }

    protected void  checkConnection () throws IOException {
        if ( this.mBeanAccessConnection == null ) {
            this.mBeanAccessConnection = this.mBeanAccessConnectionFactory.createConnection();
        }
    }

    protected void pollOneObject (Object obj)
            throws MalformedObjectNameException, IOException, ReflectionException, InvocationTargetException,
            IllegalAccessException {

        ObjectQueryInfo queryInfo = prepareObjectQuery(obj);

        if ( queryInfo != null ) {
            try {
                String[] attributeNames = new String[queryInfo.getAttributeSetters().size()];
                attributeNames = queryInfo.getAttributeSetters().keySet().toArray(attributeNames);

                //
                // Query the values now.
                //
                List<Attribute> attributeValues =
                        this.mBeanAccessConnection.getAttributes(queryInfo.getObjectName(), attributeNames);

                //
                // Finally, copy out the results.
                //
                this.copyOutAttributes(obj, attributeValues, queryInfo.getAttributeSetters(), queryInfo.objectName);
            } catch (InstanceNotFoundException infExc) {
                this.logInstanceNotFoundThrottle.info(log, "instance not found on polling object: oname={}",
                        queryInfo.getObjectName(), infExc);
            }
        }
    }

    /**
     * TBD: cache the information here
     *
     * @param obj
     * @return
     * @throws MalformedObjectNameException
     */
    protected ObjectQueryInfo prepareObjectQuery (Object obj) throws MalformedObjectNameException {

        ObjectQueryInfo result;

        //
        // Extract the mbean info from the object (TBD: cache this information ahead of time)
        //
        String onamePattern = MBeanAnnotationUtil.getLocationONamePattern(obj);

        if (onamePattern != null) {
            //
            // Locate the setters and continue only if at least one was found.
            //
            Map<String, Method> attributeSetters = MBeanAnnotationUtil.getAttributes(obj);

            if (attributeSetters.size() > 0) {
                String onameString;

                if ( obj instanceof MBeanLocationParameterSource ) {
                    onameString = this.replaceObjectNameParameters(onamePattern, (MBeanLocationParameterSource) obj);
                } else {
                    onameString = onamePattern;
                }

                ObjectName oname = new ObjectName(onameString);

                result = new ObjectQueryInfo(obj, oname, attributeSetters);
            } else {
                this.logNoAttributeThrottle.warn(log,
                        "ignoring attempt to poll MBean object with no attributes: onamePattern={}", onamePattern);
                result = null;
            }
        } else {
            log.warn("ignoring attempt to poll object that has no MBeanLocation");
            result = null;
        }

        return result;
    }

    protected void copyOutBatchAttributes (Map<ObjectName, List<Attribute>> objectAttValues,
                                           Map<ObjectName, ObjectQueryInfo> objectQueryInfo) {

        for ( Map.Entry<ObjectName, List<Attribute>> entry : objectAttValues.entrySet() ) {
            ObjectName objectName = entry.getKey();

            ObjectQueryInfo queryInfo = objectQueryInfo.get(objectName);
            this.copyOutAttributes(queryInfo.target, entry.getValue(), queryInfo.getAttributeSetters(),
                    queryInfo.getObjectName());
        }
    }

    protected void copyOutAttributes (Object target, List<Attribute> jmxAttributeValues,
                                      Map<String, Method> attributeSetters, ObjectName objectName) {

        for ( Attribute oneAttribute : jmxAttributeValues ) {
            String attributeName = oneAttribute.getName();

            Method setter = attributeSetters.get(attributeName);
            Object value = oneAttribute.getValue();

            try {
                //
                // Automatically down-convert longs to integers as-needed.
                //
                if ( ( setter.getParameterTypes()[0].isAssignableFrom(Integer.class) ) ||
                     ( setter.getParameterTypes()[0].isAssignableFrom(int.class) ) ) {

                    if ( value instanceof Long ) {
                        value = ((Long) value).intValue();
                    }
                }
                setter.invoke(target, value);
            } catch (InvocationTargetException invocationExc) {
                this.log.info("invocation exception storing mbean results: oname={}; attributeName={}", objectName,
                        attributeName, invocationExc);
            } catch (IllegalAccessException illegalAccessExc) {
                this.log.info("illegal access exception storing mbean results: oname={}; attributeName={}", objectName,
                        attributeName, illegalAccessExc);
            } catch ( IllegalArgumentException illegalArgumentExc ) {
                this.log.info("illegal argument exception storing mbean results: oname={}; attributeName={}",
                        objectName, attributeName, illegalArgumentExc);
            }
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

    protected static class ObjectQueryInfo {
        private final Object              target;
        private final ObjectName          objectName;
        private final Map<String, Method> attributeSetters;

        public ObjectQueryInfo(Object target, ObjectName objectName, Map<String, Method> attributeSetters) {
            this.target = target;
            this.objectName = objectName;
            this.attributeSetters = attributeSetters;
        }

        public ObjectName getObjectName() {
            return objectName;
        }

        public Map<String, Method> getAttributeSetters() {
            return attributeSetters;
        }

        public Set<String> getAttributeNames() {
            return attributeSetters.keySet();
        }
    }
}
