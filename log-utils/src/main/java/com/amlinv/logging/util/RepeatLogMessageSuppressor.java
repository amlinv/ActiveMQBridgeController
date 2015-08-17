package com.amlinv.logging.util;

import org.slf4j.Logger;

import java.util.Map;

/**
 * Created by art on 4/5/15.
 */
public class RepeatLogMessageSuppressor {
    private long lastLoggedTime = 0;
    private long minDelay = 300000L;

    public void debug (Logger destLogger, String message, Object... args) {
        long nowMs = System.nanoTime() / 1000000L;
        long elapsed = nowMs - lastLoggedTime;

        if ( elapsed > minDelay ) {
            destLogger.debug(message, args);
            lastLoggedTime = nowMs;
        }
    }

    public void info (Logger destLogger, String message, Object... args) {
        long nowMs = System.nanoTime() / 1000000L;
        long elapsed = nowMs - lastLoggedTime;

        if ( elapsed > minDelay ) {
            destLogger.info(message, args);
            lastLoggedTime = nowMs;
        }
    }

    public void warn (Logger destLogger, String message, Object... args) {
        long nowMs = System.nanoTime() / 1000000L;
        long elapsed = nowMs - lastLoggedTime;

        if ( elapsed > minDelay ) {
            destLogger.warn(message, args);
            lastLoggedTime = nowMs;
        }
    }

    public void error (Logger destLogger, String message, Object... args) {
        long nowMs = System.nanoTime() / 1000000L;
        long elapsed = nowMs - lastLoggedTime;

        if ( elapsed > minDelay ) {
            destLogger.warn(message, args);
            lastLoggedTime = nowMs;
        }
    }
}
