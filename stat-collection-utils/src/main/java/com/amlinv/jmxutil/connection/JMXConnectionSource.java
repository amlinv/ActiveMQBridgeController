package com.amlinv.jmxutil.connection;

import javax.management.remote.JMXConnector;
import java.io.IOException;

/**
 * Created by art on 4/1/15.
 */
public interface JMXConnectionSource {
    JMXConnector createConnection() throws IOException;
    String getTargetDescription();
}
