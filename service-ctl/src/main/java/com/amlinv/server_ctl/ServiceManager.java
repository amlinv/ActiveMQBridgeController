package com.amlinv.server_ctl;

import com.amlinv.server_ctl.exception.ServiceControlException;

import java.util.List;

/**
 * Created by art on 5/5/15.
 */
public interface ServiceManager {
    /**
     * Retrieve the list of service IDs.
     *
     * @return list of known service IDs.
     */
    List<String> getServiceIdList ();

    /**
     * Create the service with the given identifier and information.
     *
     * @param id identifier for the new service.
     * @param serviceInfo details of the service, including the commands to start and stop execution.
     * @return true => if the service is successfully added; false => if a service with the same identifier already
     * exists.
     */
    boolean createService (String id, ServiceInfo serviceInfo);

    /**
     * Lookup the service with the given identifier
     *
     * @param id identifier of the service to lookup.
     * @return information for the service with the given identifier.
     */
    ServiceInfo lookup(String id);

    /**
     * Remove the service with the given identifier.
     *
     * @param id identifier of the service to remove.
     * @return true => if the service is successfully removed; false => otherwise.
     */
    boolean removeService (String id);

    /**
     * Remove the service with the given identifier, if the service specification matches the one given.
     *
     * @param id identifier of the service to remove.
     * @param matchingInfo details of the service to remove.
     * @return true => if the service is successfully removed; false => otherwise.
     */
    boolean removeService (String id, ServiceInfo matchingInfo);

    /**
     * Start the service with the given identifier.  If the service is already executing, the details of the ongoing
     * execution will be returned.
     *
     * @param id identifier of the service to start.
     * @return details of the execution; null if the service does not exist.
     */
    ServiceExecutionInfo startService (String id) throws ServiceControlException;

    /**
     * Stop the service with the given identifier.
     * @param id identifier of the service to stop.
     * @return true => if the service was running and was stopped; false => if the service was not running.
     */
    boolean stopService (String id);
}
