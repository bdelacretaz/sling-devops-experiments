package org.apache.sling.devops.orchestrator;

import java.io.Closeable;

/** service that starts and stops minions */
public interface MinionController extends Closeable {
	/** Start a number of minions with specified config */
	void startMinions(final String config, final String crankFilePath, final int num) throws Exception;

	/** Stop all minions that currently run the specified config */
	void stopMinions(final String config) throws Exception;
}
