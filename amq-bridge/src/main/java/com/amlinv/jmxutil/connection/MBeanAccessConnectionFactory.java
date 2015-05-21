package com.amlinv.jmxutil.connection;

import java.io.IOException;

/**
 * Created by art on 5/7/15.
 */
public interface MBeanAccessConnectionFactory {
    MBeanAccessConnection createConnection() throws IOException;
    String getTargetDescription();
}
