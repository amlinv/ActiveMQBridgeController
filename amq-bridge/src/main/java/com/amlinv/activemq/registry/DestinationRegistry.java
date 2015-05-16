package com.amlinv.activemq.registry;

import com.amlinv.activemq.registry.model.DestinationInfo;
import com.amlinv.server.util.ConcurrentRegistry;

/**
 * Registry of destinations.  Note that no destination type is included in the registry, so each registry must be
 * specific to a single type of destinations.
 *
 * Created by art on 5/2/15.
 */
public class DestinationRegistry extends ConcurrentRegistry<String, DestinationInfo> {
}
