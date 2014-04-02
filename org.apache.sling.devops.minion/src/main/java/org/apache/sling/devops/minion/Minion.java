package org.apache.sling.devops.minion;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.devops.Instance;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.installer.provider.jcr.impl.JcrInstaller;
import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service
public class Minion implements StartupListener {

	private static final Logger logger = LoggerFactory.getLogger(Minion.class);

	public static final String CONFIG = System.getProperty("sling.devops.config"); // TODO
	public static final String CONFIG_PATH = "/sling-cfg";

	public static final String PID_JCR_RESOURCE_RESOLVER_FACTORY = "org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl";
	public static final String PID_JCR_INSTALLER = "org.apache.sling.installer.provider.jcr.impl.JcrInstaller";

	public static final String PATH_JCR_RESOURCE_RESOLVER_FACTORY = ResourceResolverFactoryActivator.PROP_PATH;
	public static final String PATH_JCR_INSTALLER = JcrInstaller.PROP_SEARCH_PATH;

	@Reference
	private DiscoveryService discoveryService;

	private InstanceAnnouncer instanceAnnouncer;

	@Activate
	public void onActivate() throws IOException {
		this.instanceAnnouncer = new ZooKeeperInstanceAnnouncer();
	}

	@Deactivate
	public void onDeactivate() throws Exception {
		this.instanceAnnouncer.close();
	}

	@Override
	public void inform(StartupMode mode, boolean finished) {
		if (finished) this.startupFinished(mode);
	}

	@Override
	public void startupFinished(StartupMode mode) {
		InstanceDescription instanceDescription = this.discoveryService.getTopology().getLocalInstance();
		logger.info("Startup finished, announcing config.");
		this.instanceAnnouncer.announce(new Instance(
				instanceDescription.getSlingId(),
				System.getProperty("sling.devops.config"), // TODO
				new HashSet<>(Arrays.asList(instanceDescription.getProperty(InstanceDescription.PROPERTY_ENDPOINTS).split(",")))
				));
	}

	@Override
	public void startupProgress(float ratio) {
	}
}
