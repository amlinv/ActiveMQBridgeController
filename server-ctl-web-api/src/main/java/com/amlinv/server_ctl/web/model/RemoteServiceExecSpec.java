package com.amlinv.server_ctl.web.model;

/**
 * Created by art on 5/4/15.
 */
public class RemoteServiceExecSpec extends ServiceExecSpec {
    private final String serverAddress;
    private final String username;
    private final String identity;
    private final String startCommand;
    private final String stopCommand;
    private final int port;
    private final boolean useScreen;

    public RemoteServiceExecSpec(String serverAddress, String username, String identity, String startCommand,
                                 String stopCommand, int port, boolean useScreen) {

        this.serverAddress = serverAddress;
        this.username = username;
        this.identity = identity;
        this.startCommand = startCommand;
        this.stopCommand = stopCommand;
        this.port = port;
        this.useScreen = useScreen;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getIdentity() {
        return identity;
    }

    public int getPort() {
        return port;
    }

    public boolean isUseScreen() {
        return useScreen;
    }

    public String getStartCommand() {
        return startCommand;
    }

    public String getStopCommand() {
        return stopCommand;
    }
}
