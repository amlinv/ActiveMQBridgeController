package com.amlinv.server_ctl.jsch;

import com.jcraft.jsch.Logger;

/**
 * Created by art on 5/4/15.
 */
public class Log4jAdapter implements Logger {
    private final org.slf4j.Logger slf4jLogger;

    public Log4jAdapter(org.slf4j.Logger slf4jLogger) {
        this.slf4jLogger = slf4jLogger;
    }

    @Override
    public boolean isEnabled(int level) {
        switch ( level ) {
            case DEBUG:
                return  slf4jLogger.isDebugEnabled();
            case INFO:
                return  slf4jLogger.isInfoEnabled();
            case WARN:
                return  slf4jLogger.isWarnEnabled();
            case ERROR:
                return  slf4jLogger.isErrorEnabled();
            case FATAL:
                return  slf4jLogger.isErrorEnabled();
        }

        return false;
    }

    @Override
    public void log(int level, String message) {
        switch ( level ) {
            case DEBUG:
                this.slf4jLogger.debug(message);
                break;
            case INFO:
                this.slf4jLogger.info(message);
                break;
            case WARN:
                this.slf4jLogger.warn(message);
                break;
            case ERROR:
                this.slf4jLogger.error(message);
                break;
            case FATAL:
                this.slf4jLogger.error(message);
                break;
        }
    }
}
