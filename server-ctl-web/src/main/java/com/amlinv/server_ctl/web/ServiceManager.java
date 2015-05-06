package com.amlinv.server_ctl.web;

import com.amlinv.server_ctl.web.model.LocalServiceExecSpec;
import com.amlinv.server_ctl.web.model.RemoteServiceExecSpec;
import com.amlinv.server_ctl.web.model.ServiceInfo;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by art on 5/4/15.
 */
@Path("/svcmgr")
public class ServiceManager {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ServiceManager.class);

    private Logger log = DEFAULT_LOGGER;

    private Map<String, ServiceInfo> serviceRegistry = new HashMap<>();
    private Map<String, ServiceExecution> serviceExecutions = new HashMap<>();

    // TBD999: inject or find a way to shutdown
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 5, 60, TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>());

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Path("/")
    @GET
    @Produces({ "application/json", "application/xml" })
    public Response getServiceList () {
        LinkedList<ServiceInfo> resultList = new LinkedList<>();
        synchronized ( this.serviceRegistry ) {
            resultList.addAll(this.serviceRegistry.values());
        }

        Response result = Response.ok(resultList).build();

        return  result;
    }

    // TBD:
//      - Create a service (ID, name, command, remote-server: (address, port, username, identity, useScreen), local-server)
//      - Start a service
//      - Stop a service
//      - View service log
//      - Get service status

    @Path("/{id}")
    @PUT
    @Consumes({ "application/json", "application/xml" })
    @Produces("text/plain")
    public Response createService (@PathParam("id") String id, @FormParam("service") ServiceInfo serviceInfo) {
        boolean alreadyExists;

        synchronized ( this.serviceRegistry ) {
            if ( this.serviceRegistry.containsKey(id) ) {
                alreadyExists = true;
            } else {
                this.serviceRegistry.put(id, serviceInfo);
                alreadyExists = false;
            }
        }

        Response result;
        if ( alreadyExists ) {
            result = Response.status(Response.Status.CONFLICT).entity("already exists").build();
        } else {
            result = Response.ok("created").build();
        }

        return  result;
    }

    @Path("/{id}/executions")
    @POST
    @Produces({ "application/json", "application/xml" })
    public Response startExecution (@PathParam("id") String id) {
        ServiceInfo serviceInfo;
        boolean alreadyExecuting;
        Response result;

        //
        // Grab the service information now.
        //
        synchronized ( this.serviceRegistry ) {
            serviceInfo = this.serviceRegistry.get(id);
        }

        if ( serviceInfo != null ) {
            ServiceExecution serviceExecution = this.prepareServiceExecution(serviceInfo);

            if ( serviceExecution != null ) {

                synchronized (this.serviceExecutions) {
                    if (this.serviceExecutions.containsKey(id)) {
                        alreadyExecuting = true;
                    } else {
                        alreadyExecuting = false;
                        this.serviceExecutions.put(id, serviceExecution);
                    }
                }

                if (!alreadyExecuting) {
                    result = Response.ok(serviceExecution).build();
                    this.queueStartServiceExecution(serviceExecution);
                } else {
                    result = Response.status(Response.Status.CONFLICT).entity("service already executing").build();
                }
            } else {
                result = Response.status(Response.Status.BAD_REQUEST).entity("invalid service execution specification")
                        .build();
            }
        } else {
            result = Response.status(Response.Status.NOT_FOUND).entity("service not found").build();
        }

        return  result;
    }

    // TBD: move this to a separate controller class
    protected ServiceExecution prepareServiceExecution (ServiceInfo serviceInfo) {
        ServiceExecution result;

        if ( serviceInfo.getExecSpec() instanceof RemoteServiceExecSpec ) {
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

    // TBD: move this to a separate controller class; should be easy to use as an embedded service
    protected void queueStartServiceExecution(final ServiceExecution execution) {
        this.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                executeService(execution);
            }
        });
    }

    // TBD: move this to a separate execution class
    protected void executeService (ServiceExecution execution) {
        this.log.info("time to start execution");

        if ( execution instanceof RemoteServiceExecution ) {
            RemoteServiceExecution remoteExec = (RemoteServiceExecution) execution;

            try {
                // TBD999: start capturing output
                // TBD999: do something with the input (redirect from /dev/null?)
                remoteExec.getRemoteServer().start();
            } catch (JSchException | IOException startExc) {
                this.log.error("error starting remote server", startExc);
            }
        }
    }
}
