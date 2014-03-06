package org.apache.sling.devops.orchestrator;

import java.util.Set;

public interface ConfigTransitioner {

	public void transition(String config, Set<String> instances) throws Exception;
}
