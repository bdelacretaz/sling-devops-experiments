package org.apache.sling.devops.minion;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true) // needed for @Reference below to work
public abstract class ConfigAnnouncer implements StartupListener {

	private static final Logger logger = LoggerFactory.getLogger(ConfigAnnouncer.class);

	@Reference
	private DiscoveryService discoveryService;

	public String getSlingId() {
		return this.discoveryService.getTopology().getLocalInstance().getSlingId();
	}

	public String getEndpoints() {
		return this.discoveryService.getTopology().getLocalInstance().getProperty(InstanceDescription.PROPERTY_ENDPOINTS);
		//return this.propertyProvider.getProperty(InstanceDescription.PROPERTY_ENDPOINTS);
	}

	public String getConfig() {
		return System.getProperty("sling.devops.config"); // TODO
	}

	public abstract void announceConfig();

	@Override
	public void inform(StartupMode mode, boolean finished) {
		logger.info("inform mode={}, finished={}", mode, finished);
		if (finished) this.startupFinished(mode);
	}

	@Override
	public void startupFinished(StartupMode mode) {
		logger.info("startupFinished mode={}", mode);
		this.announceConfig();
	}

	@Override
	public void startupProgress(float ratio) {
		logger.info("startupProgress ratio={}", ratio);
	}
}
