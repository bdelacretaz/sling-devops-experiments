package org.apache.sling.devops;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Instance {

	private final String id;
	private final String config;
	private final Set<String> endpoints;

	public Instance(String id, String config, Set<String> endpoints) {
		if (id == null) throw new IllegalArgumentException("ID cannot be null.");
		if (config == null) throw new IllegalArgumentException("Config cannot be null.");
		if (endpoints == null) throw new IllegalArgumentException("Endpoints cannot be null.");
		for (String endpoint : endpoints) {
			if (endpoint == null) throw new IllegalArgumentException("No endpoint can be null.");
		}
		this.id = id;
		this.config = config;
		this.endpoints = Collections.unmodifiableSet(new HashSet<>(endpoints));
	}

	public String getId() {
		return this.id;
	}

	public String getConfig() {
		return this.config;
	}

	public Set<String> getEndpoints() {
		return this.endpoints;
	}
}
