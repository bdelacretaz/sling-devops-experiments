package org.apache.sling.devops.orchestrator;

import java.io.Closeable;

import org.apache.sling.devops.Instance;

public interface InstanceMonitor extends Closeable {
	public void addInstanceListener(InstanceListener listener);

	public interface InstanceListener {
		public void onInstanceAdded(Instance instance);
		public void onInstanceChanged(Instance instance);
		public void onInstanceRemoved(String slingId);
	}
}
