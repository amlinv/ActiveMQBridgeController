package com.amlinv.server_ctl.web.model;

/**
 * Created by art on 5/4/15.
 */
public class ServiceInfo {
    private String name;
    private ServiceExecSpec execSpec;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceExecSpec getExecSpec() {
        return execSpec;
    }

    public void setExecSpec(ServiceExecSpec execSpec) {
        this.execSpec = execSpec;
    }
}
