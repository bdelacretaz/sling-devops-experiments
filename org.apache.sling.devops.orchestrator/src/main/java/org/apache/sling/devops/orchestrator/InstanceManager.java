package org.apache.sling.devops.orchestrator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.devops.Instance;

public class InstanceManager {

	private final Map<String, Instance> instances = new HashMap<>();
	private final Map<String, String> bestEndpoints = new HashMap<>();
	private final Map<String, Set<String>> configEndpoints = new HashMap<>();

	public Set<String> getEndpoints(String config) {
		if (!this.configEndpoints.containsKey(config)) return new HashSet<String>();
		return this.configEndpoints.get(config);
	}

	public void addInstance(Instance instance) {
		if (instance == null) throw new IllegalArgumentException("Instance cannot be null.");
		if (instance.getEndpoints().isEmpty()) return;
		String bestEndpoint = this.pickBestEndpoint(instance.getEndpoints());
		if (bestEndpoint != null) {
			this.instances.put(instance.getId(), instance);
			this.bestEndpoints.put(instance.getId(), bestEndpoint);
			String config = instance.getConfig();
			if (!this.configEndpoints.containsKey(config)) {
				this.configEndpoints.put(config, new HashSet<String>());
			}
			this.configEndpoints.get(config).add(bestEndpoint);
		}
	}

	public void removeInstance(String id) {
		if (id == null) throw new IllegalArgumentException("ID cannot be null.");
		Instance instance = this.instances.remove(id);
		String bestEndpoint = this.bestEndpoints.remove(id);
		String config = instance.getConfig();
		if (instance != null && this.configEndpoints.containsKey(config)) {
			this.configEndpoints.get(config).remove(bestEndpoint);
			if (this.configEndpoints.get(config).isEmpty()) {
				this.configEndpoints.remove(config);
			}
		}
	}

	public Map<String, Set<String>> getConfigs() {
		return Collections.unmodifiableMap(this.configEndpoints);
	}

	private String pickBestEndpoint(Set<String> endpoints) {
		return endpoints.iterator().next(); // TODO
	}
}
