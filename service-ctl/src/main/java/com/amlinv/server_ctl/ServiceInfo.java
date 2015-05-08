package com.amlinv.server_ctl;

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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        ServiceInfo that = (ServiceInfo) other;

        if (!execSpec.equals(that.execSpec)) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + execSpec.hashCode();
        return result;
    }
}
