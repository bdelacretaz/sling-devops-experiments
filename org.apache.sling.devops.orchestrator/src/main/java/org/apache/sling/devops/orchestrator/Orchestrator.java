package org.apache.sling.devops.orchestrator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.devops.Instance;
import org.apache.sling.devops.orchestrator.git.GitFileMonitor;
import org.apache.sling.devops.orchestrator.git.LocalGitFileMonitor;
import org.apache.sling.devops.orchestrator.git.RemoteGitFileMonitor;
import org.apache.sling.settings.SlingSettingsService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
public class Orchestrator {

	private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);

	public static final String DEVOPS_DIR = "devops";
	public static final String GIT_REPO_PATH_PROP = "sling.devops.git.repo";
	public static final String GIT_REPO_FILE_PATH_PROP = "sling.devops.git.file";
	public static final String GIT_WORKING_COPY_DIR = DEVOPS_DIR + "/repo";
	public static final String PROXY_EXECUTABLE_PROP = "sling.devops.proxy.executable";
	public static final String PROXY_EXECUTABLE_DEFAULT = "apachectl";
	public static final String PROXY_CONFIG_PATH_PROP = "sling.devops.proxy.configPath";
	public static final String PROXY_CONFIG_PATH_DEFAULT = "/private/etc/apache2/mod_proxy_balancer.conf";
	public static final String SUDO_PASSWORD_PROP = "sudo.password";
	public static final String N_PROP = "sling.devops.orchestrator.n";
	public static final int N_DEFAULT = 2;

	@Reference
	private SlingSettingsService slingSettingsService;

	private File devopsDirectory;
	private int n;
	private InstanceListener instanceListener;
	private InstanceManager instanceManager;
	private GitFileMonitor gitFileMonitor;
	private ConfigTransitioner configTransitioner;
	private String currentConfig = "";

	@Activate
	public void onActivate(BundleContext bundleContext) throws GitAPIException, IOException, InterruptedException {
		this.n = PropertiesUtil.toInteger(bundleContext.getProperty(N_PROP), N_DEFAULT);
		this.instanceManager = new InstanceManager();

		this.devopsDirectory = new File(this.slingSettingsService.getAbsolutePathWithinSlingHome(DEVOPS_DIR));
		if (!this.devopsDirectory.exists()) this.devopsDirectory.mkdir();

		// Setup Git monitor
		final String gitRepoPath = PropertiesUtil.toString(bundleContext.getProperty(GIT_REPO_PATH_PROP), null);
		final String gitRepoFilePath = PropertiesUtil.toString(bundleContext.getProperty(GIT_REPO_FILE_PATH_PROP), null);
		if (gitRepoPath.contains("://")) { // assume remote
			this.gitFileMonitor = new RemoteGitFileMonitor(
					gitRepoPath,
					this.slingSettingsService.getAbsolutePathWithinSlingHome(GIT_WORKING_COPY_DIR),
					gitRepoFilePath
					);
		} else {
			this.gitFileMonitor = new LocalGitFileMonitor(gitRepoPath, gitRepoFilePath);
		}
		this.gitFileMonitor.addListener(new GitFileMonitor.GitFileListener() {
			@Override
			public void onModified(long time, ByteBuffer content) {
				final String config = "C" + time;
				if (!Orchestrator.this.tryTransition(config)) {
					final File configFile = new File(Orchestrator.this.devopsDirectory, config + ".crank.txt");
					try (final FileChannel fileChannel = new FileOutputStream(configFile, false).getChannel()) {
						fileChannel.write(content);
						logger.info(
								"CRANKSTART {} MORE INSTANCES FROM {} WITH config={}.",
								Orchestrator.this.n - Orchestrator.this.instanceManager.getEndpoints(config).size(),
								configFile.getAbsolutePath(),
								config
								);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						logger.error("Could not write crank.txt file.", e);
					}
				}
			}
		});
		this.gitFileMonitor.start();

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
				Orchestrator.this.tryTransition(instance.getConfig());
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
		this.gitFileMonitor.close();
		this.instanceListener.close();
	}

	private boolean tryTransition(String newConfig) {
		if (!this.isConfigNewer(newConfig)) return true;
		else if (this.isConfigSatisfied(newConfig)) {
			logger.info("Config {} satisfied, transitioning.", newConfig);
			try {
				this.configTransitioner.transition(
						newConfig,
						this.instanceManager.getEndpoints(newConfig)
						);
				this.currentConfig = newConfig;
				return true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.error("Transition failed.", e);
			}
		}
		return false;
	}

	private boolean isConfigNewer(String newConfig) {
		return newConfig.compareTo(this.currentConfig) > 0;
	}

	private boolean isConfigSatisfied(String newConfig) {
		return this.instanceManager.getEndpoints(newConfig).size() >= this.n;
	}
}
