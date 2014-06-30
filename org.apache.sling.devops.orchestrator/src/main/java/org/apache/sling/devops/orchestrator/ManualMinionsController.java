package org.apache.sling.devops.orchestrator;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default MinionsController that just asks the user to start/stop minions.
 */
@Component
@Service
public class ManualMinionsController implements MinionsController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void startMinions(String config, String crankFilePath, int num) {
		logger.info("PLEASE CRANKSTART {} MINIONS WITH config={} FROM {}.",
				num,
				config,
				crankFilePath
				);
	}

	@Override
	public void stopMinions(String config, Set<String> endPoints) {
		logger.info("PLEASE STOP MINIONS HAVING config={}: {}",
				config,
				endPoints
				);
	}
}
