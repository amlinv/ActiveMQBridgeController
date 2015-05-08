package com.amlinv.server_ctl;

/**
 * Created by art on 5/4/15.
 */
public class LocalServiceExecSpec extends ServiceExecSpec {
    private final String startCommand;
    private final String stopCommand;
    private final boolean useScreen;

    public LocalServiceExecSpec(String startCommand, String stopCommand, boolean useScreen) {
        this.startCommand = startCommand;
        this.stopCommand = stopCommand;
        this.useScreen = useScreen;
    }

    public String getStartCommand() {
        return startCommand;
    }

    public String getStopCommand() {
        return stopCommand;
    }

    public boolean isUseScreen() {
        return useScreen;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        LocalServiceExecSpec that = (LocalServiceExecSpec) other;

        if (useScreen != that.useScreen) return false;
        if (!startCommand.equals(that.startCommand)) return false;
        if (!stopCommand.equals(that.stopCommand)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startCommand.hashCode();
        result = 31 * result + stopCommand.hashCode();
        result = 31 * result + (useScreen ? 1 : 0);
        return result;
    }
}
