package com.amlinv.server_ctl;

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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        RemoteServiceExecSpec that = (RemoteServiceExecSpec) other;

        if (port != that.port) return false;
        if (useScreen != that.useScreen) return false;
        if (!identity.equals(that.identity)) return false;
        if (!serverAddress.equals(that.serverAddress)) return false;
        if (!startCommand.equals(that.startCommand)) return false;
        if (!stopCommand.equals(that.stopCommand)) return false;
        if (!username.equals(that.username)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = serverAddress.hashCode();
        result = 31 * result + username.hashCode();
        result = 31 * result + identity.hashCode();
        result = 31 * result + startCommand.hashCode();
        result = 31 * result + stopCommand.hashCode();
        result = 31 * result + port;
        result = 31 * result + (useScreen ? 1 : 0);
        return result;
    }
}
