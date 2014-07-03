package org.apache.sling.devops.orchestrator;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Orchestrator {
	public int getN();
	public String getActiveConfig();
	public String getTargetConfig();
	public Map<String, Set<String>> getConfigs();
	public List<String> getLog();
}
