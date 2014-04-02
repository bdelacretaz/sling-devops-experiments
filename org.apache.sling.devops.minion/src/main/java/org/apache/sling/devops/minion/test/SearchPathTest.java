package org.apache.sling.devops.minion.test;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.devops.minion.Minion;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(SlingAnnotationsTestRunner.class)
public class SearchPathTest {

	@TestReference
	private ConfigurationAdmin configurationAdmin;

	@Test
	public void checkConfigurationAdmin() {
		Assert.assertNotNull("ConfigurationAdmin is null.", this.configurationAdmin);
	}

	@Test
	public void checkJcrResourceResolverFactoryPaths() throws IOException {
		Assume.assumeNotNull(Minion.CONFIG);
		this.checkPaths(Minion.PID_JCR_RESOURCE_RESOLVER_FACTORY, Minion.PATH_JCR_RESOURCE_RESOLVER_FACTORY);
	}

	@Test
	public void checkJcrInstallerPaths() throws IOException {
		Assume.assumeNotNull(Minion.CONFIG);
		this.checkPaths(Minion.PID_JCR_INSTALLER, Minion.PATH_JCR_INSTALLER);
	}

	private void checkPaths(String pid, String propertyName) throws IOException {
		Assert.assertNotNull("Config property is null.", Minion.CONFIG);
		final Configuration config = this.configurationAdmin.getConfiguration(pid);
		Assert.assertNotNull("Config object is null.", config);
		final Dictionary<?, ?> properties = config.getProperties();
		Assert.assertNotNull("Properties are null.", properties);
		final String[] paths = PropertiesUtil.toStringArray(config.getProperties().get(propertyName));
		Assert.assertNotNull("Paths array is null.", paths);
		final String expectedPathStart = Minion.CONFIG_PATH + '/' + Minion.CONFIG;
		for (final String path : paths) {
			Assert.assertTrue(
					String.format("Path %s does not start with %s.", path, expectedPathStart),
					path.startsWith(expectedPathStart)
					);
		}
	}
}
