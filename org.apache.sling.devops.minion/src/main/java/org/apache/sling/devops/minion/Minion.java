package org.apache.sling.devops.minion;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.devops.Instance;
import org.apache.sling.devops.minion.test.SearchPathTest;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.installer.provider.jcr.impl.JcrInstaller;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
public class Minion {

	private static final Logger logger = LoggerFactory.getLogger(Minion.class);

	public static final String CONFIG_PROP = "sling.devops.config";
	public static final String CONFIG_PATH = "/sling-cfg";

	public static final String PID_JUNIT_HEALTH_CHECK = "org.apache.sling.junit.healthcheck.impl.JUnitHealthCheck";
	public static final String PID_JCR_RESOURCE_RESOLVER_FACTORY = "org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl";
	public static final String PID_JCR_INSTALLER = "org.apache.sling.installer.provider.jcr.impl.JcrInstaller";

	public static final String PATH_JCR_RESOURCE_RESOLVER_FACTORY = ResourceResolverFactoryActivator.PROP_PATH;
	public static final String PATH_JCR_INSTALLER = JcrInstaller.PROP_SEARCH_PATH;

	public static final String[] HC_TAGS = new String[]{ "devops", "minion" };
	public static final String HC_PACKAGE = SearchPathTest.class.getPackage().getName();

	@Property(label = "ZooKeeper connection string")
	public static final String ZK_CONNECTION_STRING_PROP = "sling.devops.zookeeper.connString";

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	private HealthCheckExecutor healthCheckExecutor;

	@Reference
	private DiscoveryService discoveryService;

	private String config;
	private InstanceAnnouncer instanceAnnouncer;
	private HealthCheckMonitor healthCheckMonitor;

	@Activate
	public void onActivate(final ComponentContext componentContext) throws IOException, InvalidSyntaxException {
		final Dictionary<?, ?> properties = componentContext.getProperties();
		this.config = componentContext.getBundleContext().getProperty(CONFIG_PROP);
		this.instanceAnnouncer = new ZooKeeperInstanceAnnouncer(
				PropertiesUtil.toString(properties.get(ZK_CONNECTION_STRING_PROP), null));

		// Check health check config
		final Configuration[] hcConfigs = this.configurationAdmin.listConfigurations(String.format(
				"(%s=%s)",
				ConfigurationAdmin.SERVICE_FACTORYPID,
				PID_JUNIT_HEALTH_CHECK
				));
		boolean hcConfigExists = false;
		if (hcConfigs != null) {
			for (final Configuration hcConfig : hcConfigs) {
				final Dictionary<?, ?> hcConfigProps = hcConfig.getProperties();
				if (hcConfigProps != null) {
					final Object hcConfigPackage = hcConfigProps.get("test.package.or.class");
					if (hcConfigPackage != null && hcConfigPackage.equals(HC_PACKAGE)) {
						hcConfigExists = true;
						break;
					}
				}
			}
		}
		if (!hcConfigExists) {
			final Configuration hcConfig = this.configurationAdmin.createFactoryConfiguration(PID_JUNIT_HEALTH_CHECK, null);
			final Dictionary<String, Object> props = new Hashtable<>();
			props.put(HealthCheck.NAME, "DevOps Minion Health Checks");
			props.put(HealthCheck.TAGS, HC_TAGS);
			props.put("test.package.or.class", HC_PACKAGE);
			hcConfig.update(props);
			logger.info("Created health check configuration.");
		}

		// Initialize health check monitor
		this.healthCheckMonitor = new HealthCheckMonitor(this.healthCheckExecutor, HC_TAGS);
		this.healthCheckMonitor.addListener(new HealthCheckMonitor.HealthCheckListener() {
			@Override
			public void onOk() {
				logger.info("Health checks succeeded, announcing instance...");
				final InstanceDescription instanceDescription = Minion.this.discoveryService.getTopology().getLocalInstance();
				Minion.this.instanceAnnouncer.announce(new Instance(
						instanceDescription.getSlingId(),
						Minion.this.config,
						new HashSet<>(Arrays.asList(instanceDescription.getProperty(InstanceDescription.PROPERTY_ENDPOINTS).split(",")))
						));
			}
			@Override
			public void onFail() {}
		});
		this.healthCheckMonitor.start();

		logger.info("Activated with config {}.", this.config);
	}

	@Deactivate
	public void onDeactivate() throws Exception {
		this.instanceAnnouncer.close();
		this.healthCheckMonitor.close();
	}
}
