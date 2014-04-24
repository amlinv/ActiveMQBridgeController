package com.amlinv.util.service;

/**
 * API for classes that can be controlled by a ServiceController.
 *
 * Created by art on 4/23/14.
 */
public interface Service {
    /**
     * Obtain a service name for reporting purposes.
     *
     * @return name of the service.
     */
    String  getServiceName();

    /**
     * Indicate whether this service can run multiple times, or only a single time.
     *
     * @return true => the service can safely be started and stopped more than once; false => the service
     * must only be started and stopped one time.
     */
    boolean canRunMultipleTimes();

    /**
     * Start the service given that it has not already been started.
     *
     * @throws Exception - if the service fails to start.
     */
    void    startService() throws Exception;

    /**
     * Stop the service given that it is currently running.
     *
     * @throws Exception - if the service fails to shutdown cleanly.
     */
    void    stopService()  throws Exception;
}
