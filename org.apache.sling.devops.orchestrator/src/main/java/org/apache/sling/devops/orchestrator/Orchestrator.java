package org.apache.sling.devops.orchestrator;

import java.io.IOException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.devops.Instance;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
public class Orchestrator {

	private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);

	public static final String PROXY_EXECUTABLE_PROP = "sling.devops.proxy.executable";
	public static final String PROXY_EXECUTABLE_DEFAULT = "apachectl";
	public static final String PROXY_CONFIG_PATH_PROP = "sling.devops.proxy.configPath";
	public static final String PROXY_CONFIG_PATH_DEFAULT = "/private/etc/apache2/mod_proxy_balancer.conf";
	public static final String SUDO_PASSWORD_PROP = "sudo.password";
	public static final String N_PROP = "sling.devops.orchestrator.n";
	public static final int N_DEFAULT = 2;
	private int n;

	private InstanceListener instanceListener;
	private InstanceManager instanceManager;
	private ConfigTransitioner configTransitioner;
	private String currentConfig = "";

	@Activate
	public void onActivate(BundleContext bundleContext) throws IOException, InterruptedException {
		this.n = PropertiesUtil.toInteger(bundleContext.getProperty(N_PROP), N_DEFAULT);
		this.instanceManager = new InstanceManager();
		this.configTransitioner = new ModProxyConfigTransitioner(
				PropertiesUtil.toString(bundleContext.getProperty(PROXY_EXECUTABLE_PROP), PROXY_EXECUTABLE_DEFAULT),
				PropertiesUtil.toString(bundleContext.getProperty(PROXY_CONFIG_PATH_PROP), PROXY_CONFIG_PATH_DEFAULT),
				(String)bundleContext.getProperty(SUDO_PASSWORD_PROP)
				);
		this.instanceListener = new ZooKeeperInstanceListener(
				(String)bundleContext.getProperty(ZooKeeperInstanceListener.ZK_CONNECTION_STRING_PROP)) {

			@Override
			public void onInstanceAdded(Instance instance) {
				Orchestrator.this.instanceManager.addInstance(instance);
				String newConfig = instance.getConfig();
				if (Orchestrator.this.isConfigSatisfied(newConfig)) {
					logger.info("Config {} satisfied, transitioning.", newConfig);
					try {
						Orchestrator.this.configTransitioner.transition(
								newConfig,
								Orchestrator.this.instanceManager.getEndpoints(newConfig)
								);
						Orchestrator.this.currentConfig = newConfig;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onInstanceChanged(Instance instance) {
				this.onInstanceRemoved(instance.getId());
				this.onInstanceAdded(instance);
			}

			@Override
			public void onInstanceRemoved(String slingId) {
				Orchestrator.this.instanceManager.removeInstance(slingId);
			}
		};
	}

	@Deactivate
	public void onDeactivate() throws Exception {
		this.instanceListener.close();
	}

	private boolean isConfigSatisfied(String newConfig) {
		return newConfig.compareTo(this.currentConfig) >= 0 &&
				this.instanceManager.getEndpoints(newConfig).size() >= this.n;
	}
}
