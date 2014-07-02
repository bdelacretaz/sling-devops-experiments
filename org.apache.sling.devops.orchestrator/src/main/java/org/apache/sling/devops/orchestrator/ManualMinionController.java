package org.apache.sling.devops.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default MinionsController that just asks the user to start/stop minions.
 */
public class ManualMinionController implements MinionController {

	private static final Logger logger = LoggerFactory.getLogger(ManualMinionController.class);

	@Override
	public void startMinions(final String config, final String crankFilePath, final int num) {
		logger.info("PLEASE CRANKSTART {} MINIONS WITH config={} FROM {}.",
				num,
				config,
				crankFilePath
				);
	}

	@Override
	public void stopMinions(final String config) {
		logger.info("PLEASE STOP MINIONS HAVING config={}", config);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}
}
