package com.amlinv.server_ctl.sapphire;

import com.amlinv.registry.util.ConcurrentRegistry;
import com.amlinv.server_ctl.LocalServiceExecSpec;
import com.amlinv.server_ctl.LocalServiceExecution;
import com.amlinv.server_ctl.RemoteServiceExecSpec;
import com.amlinv.server_ctl.RemoteServiceExecution;
import com.amlinv.server_ctl.ServiceExecution;
import com.amlinv.server_ctl.ServiceExecutionInfo;
import com.amlinv.server_ctl.ServiceInfo;
import com.amlinv.server_ctl.ServiceManager;
import com.amlinv.server_ctl.exception.ServiceControlException;
import com.amlinv.server_ctl.exception.ServiceExecutionSpecificationInvalidException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by art on 5/5/15.
 */
public class SapphireServiceManager implements ServiceManager {
    private final ConcurrentRegistry<String, ServiceInfo> serviceRegistry;
    private final ConcurrentRegistry<String, ServiceExecutionInfo> executionRegistry;

    public SapphireServiceManager(ConcurrentRegistry<String, ServiceInfo> serviceRegistry,
                                  ConcurrentRegistry<String, ServiceExecutionInfo> executionRegistry) {

        this.serviceRegistry = serviceRegistry;
        this.executionRegistry = executionRegistry;
    }

    @Override
    public List<String> getServiceIdList() {
        return new LinkedList<>(this.serviceRegistry.keys());
    }

    @Override
    public ServiceInfo lookup(String id) {
        return this.serviceRegistry.get(id);
    }

    @Override
    public boolean createService(String id, ServiceInfo serviceInfo) {
        boolean result;

        // TBD999: validate the service execution specification and other details
        if ( this.serviceRegistry.putIfAbsent(id, serviceInfo) == null ) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    @Override
    public boolean removeService(String id) {
        boolean result;
        if ( this.serviceRegistry.remove(id) != null ) {
            result = true;
        } else {
            result = false;
        }

        return result;
    }

    @Override
    public boolean removeService(String id, ServiceInfo matchingInfo) {
        boolean result;

        result = this.serviceRegistry.remove(id, matchingInfo);

        return result;
    }

    @Override
    public ServiceExecutionInfo startService(String id) throws ServiceControlException {
        ServiceExecutionInfo result;

        ServiceInfo serviceInfo = this.serviceRegistry.get(id);

        if ( serviceInfo != null ) {
            // TBD999: lookup existing execution (or use putIfAbsent later?)
            ServiceExecution serviceExecution = this.prepareServiceExecution(serviceInfo);

            // TBD

            if ( serviceExecution != null ) {
                result = new ServiceExecutionInfo("TBD");

                // TBD999: queue the execution now
            } else {
                throw new ServiceExecutionSpecificationInvalidException("invalid service execution specification");
            }
        } else {
            result = null;
        }

        return result;
    }

    @Override
    public boolean stopService(String id) {
        // TBD999
        return false;
    }

    protected ServiceExecution prepareServiceExecution (ServiceInfo serviceInfo) {
        ServiceExecution result;

        if ( serviceInfo.getExecSpec() instanceof RemoteServiceExecSpec) {
            RemoteServiceExecSpec spec = (RemoteServiceExecSpec) serviceInfo.getExecSpec();
            RemoteServiceExecution remoteResult = new RemoteServiceExecution(spec);

            result = remoteResult;
        } else if ( serviceInfo.getExecSpec() instanceof LocalServiceExecSpec) {
            LocalServiceExecSpec spec = (LocalServiceExecSpec) serviceInfo.getExecSpec();
            LocalServiceExecution localResult = new LocalServiceExecution(spec);

            result = localResult;
        } else {
            result = null;
        }

        return  result;
    }
}
