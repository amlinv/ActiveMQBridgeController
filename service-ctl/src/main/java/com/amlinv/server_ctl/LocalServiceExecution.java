package com.amlinv.server_ctl;

/**
 * Created by art on 5/4/15.
 */
public class LocalServiceExecution extends ServiceExecution {
    private final LocalServiceExecSpec spec;

    // TBD: state for commons exec

    public LocalServiceExecution(LocalServiceExecSpec spec) {
        this.spec = spec;
    }

    public LocalServiceExecSpec getSpec() {
        return spec;
    }

    // TBD: access to the process inputs and outputs...
    // TBD: access to the indication of service still active or not...
}
