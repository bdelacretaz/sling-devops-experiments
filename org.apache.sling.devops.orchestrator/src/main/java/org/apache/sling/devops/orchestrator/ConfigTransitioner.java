package org.apache.sling.devops.orchestrator;

import java.io.Closeable;
import java.util.Set;

public interface ConfigTransitioner extends Closeable {

	public void transition(String config, Set<String> endpoints) throws Exception;
}
