package org.apache.sling.devops.orchestrator;

import org.apache.sling.devops.Instance;

public interface InstanceListener extends AutoCloseable {

	public void onInstanceAdded(Instance instance);
	public void onInstanceChanged(Instance instance);
	public void onInstanceRemoved(String slingId);
}
