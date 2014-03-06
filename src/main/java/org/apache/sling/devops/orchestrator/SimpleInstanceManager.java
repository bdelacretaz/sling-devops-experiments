package org.apache.sling.devops.orchestrator;

public abstract class SimpleInstanceManager extends InstanceManager {

	private final int n;

	public SimpleInstanceManager(int n) {
		this.n = n;
	}

	@Override
	public boolean isConfigSatisfied(String config) {
		return this.getEndpoints(config).size() >= n;
	}
}
