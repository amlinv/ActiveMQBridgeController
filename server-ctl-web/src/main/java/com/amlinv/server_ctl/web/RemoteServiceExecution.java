package com.amlinv.server_ctl.web;

import com.amlinv.server_ctl.RemoteServer;
import com.amlinv.server_ctl.web.model.RemoteServiceExecSpec;

/**
 * Created by art on 5/4/15.
 */
public class RemoteServiceExecution extends ServiceExecution {
    private final RemoteServiceExecSpec spec;

    private final RemoteServer remoteServer;
    // TBD: JSch stuff from server-ctl

    public RemoteServiceExecution(RemoteServiceExecSpec spec) {
        this.spec = spec;

        this.remoteServer = new RemoteServer();
        this.remoteServer.setCommand(spec.getStartCommand());
        this.remoteServer.setServer(spec.getServerAddress());
        this.remoteServer.setSshPort(spec.getPort());
        this.remoteServer.setUsername(spec.getUsername());
        this.remoteServer.setConnectTimeout(60000); // TBD: configurable
    }

    public RemoteServiceExecSpec getSpec() {
        return spec;
    }

    public RemoteServer getRemoteServer() {
        return remoteServer;
    }
}
