package com.amlinv.server_ctl.web;

import com.amlinv.server_ctl.RemoteServiceExecution;
import com.amlinv.server_ctl.ServiceExecution;
import com.amlinv.server_ctl.ServiceExecutionInfo;
import com.amlinv.server_ctl.ServiceInfo;
import com.amlinv.server_ctl.ServiceManager;
import com.amlinv.server_ctl.exception.ServiceControlException;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by art on 5/4/15.
 */
@Path("/svcmgr")
public class ServiceWebManager {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ServiceWebManager.class);

    private Logger log = DEFAULT_LOGGER;

    private ServiceManager serviceManager;
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

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @Path("/")
    @GET
    @Produces({ "application/json", "application/xml" })
    public Response getServiceList () {
        Map<String, ServiceInfo> details = new TreeMap<>();

        List<String> idList = this.serviceManager.getServiceIdList();
        for ( String oneId : idList ) {
            ServiceInfo serviceInfo = this.serviceManager.lookup(oneId);

            // Check for null in case the registry changed underneath us.
            if ( serviceInfo != null ) {
                details.put(oneId, serviceInfo);
            }
        }

        Response result = Response.ok(details).build();

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
        Response result;
        if ( this.serviceManager.createService(id, serviceInfo) ) {
            result = Response.ok("created").build();
        } else {
            result = Response.status(Response.Status.CONFLICT).entity("already exists").build();
        }

        return  result;
    }

    @Path("/{id}/executions")
    @POST
    @Produces({ "application/json", "application/xml" })
    public Response startExecution (@PathParam("id") String id) {
        Response result;

        ServiceExecutionInfo execInfo = null;
        try {
            execInfo = this.serviceManager.startService(id);

            if ( execInfo != null ) {
                result = Response.ok(execInfo).build();
            } else {
                result = Response.status(Response.Status.NOT_FOUND).entity("service not found").build();
            }
        } catch (ServiceControlException e) {
            result = Response.status(Response.Status.BAD_REQUEST).entity("invalid service execution specification")
                    .build();
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

        if ( execution instanceof RemoteServiceExecution) {
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
