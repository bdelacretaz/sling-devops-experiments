package org.apache.sling.devops.minion;

import org.apache.sling.devops.Instance;

public interface InstanceAnnouncer extends AutoCloseable {

	public void announce(Instance instance);
}
