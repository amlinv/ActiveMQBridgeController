package com.amlinv.server_ctl.exception;

/**
 * Created by art on 5/6/15.
 */
public class ServiceControlException extends Exception {
    public ServiceControlException() {
        super();
    }

    public ServiceControlException(String message) {
        super(message);
    }

    public ServiceControlException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceControlException(Throwable cause) {
        super(cause);
    }

    protected ServiceControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
