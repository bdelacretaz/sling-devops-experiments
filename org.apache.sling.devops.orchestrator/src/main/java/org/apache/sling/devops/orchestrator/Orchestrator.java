package org.apache.sling.devops.orchestrator;

import java.io.IOException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.devops.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
public class Orchestrator {

	private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);

	public static final String PROXY_EXECUTABLE;
	public static final String PROXY_CONFIG_PATH;
	public static final String SUDO_PASSWORD = System.getProperty("sudo.password");
	public static final int N;
	static { // TODO
		String proxyExecutable = System.getProperty("sling.devops.proxy.executable");
		PROXY_EXECUTABLE = proxyExecutable == null ? "apachectl" : proxyExecutable;
		String proxyConfigPath = System.getProperty("sling.devops.proxy.configPath");
		PROXY_CONFIG_PATH = proxyConfigPath == null ? "/private/etc/apache2/mod_proxy_balancer.conf" : proxyConfigPath;
		String n = System.getProperty("sling.devops.orchestrator.n");
		N = n == null ? 2 : Integer.parseInt(n);
	}

	private InstanceListener instanceListener;
	private InstanceManager instanceManager;
	private ConfigTransitioner configTransitioner;
	private String currentConfig = "";

	@Activate
	public void onActivate() throws IOException {
		this.instanceManager = new InstanceManager();
		this.configTransitioner = new ModProxyConfigTransitioner(PROXY_EXECUTABLE, PROXY_CONFIG_PATH, SUDO_PASSWORD);
		this.instanceListener = new ZooKeeperInstanceListener() {

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
				this.instanceManager.getEndpoints(newConfig).size() >= N;
	}
}
