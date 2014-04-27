package com.amlinv.util.thread;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {
    private int     num = 1;
    private String  namePrefix = "daemon-thread-";

    public DaemonThreadFactory (String prefix) {
        this.namePrefix = prefix;
    }

    public Thread newThread (Runnable run) {
        Thread result = new Thread(run);

        result.setDaemon(true);
        result.setName(namePrefix + num++);

        return  result;
    }
}
