package com.amlinv.server_ctl.web.model;

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
}
