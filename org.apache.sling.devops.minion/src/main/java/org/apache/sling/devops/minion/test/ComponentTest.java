package org.apache.sling.devops.minion.test;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.sling.devops.minion.Minion;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SlingAnnotationsTestRunner.class)
public class ComponentTest {

	@TestReference
	private ScrService scrService;

	@Test
	public void checkScrService() {
		Assert.assertNotNull("SCR Service is null.", this.scrService);
	}

	@Test
	public void checkJcrResourceResolverFactoryImpl() {
		this.checkComponentActive(Minion.PID_JCR_RESOURCE_RESOLVER_FACTORY, "JCR Resource Resolver Factory");
	}

	@Test
	public void checkJcrInstaller() {
		this.checkComponentActive(Minion.PID_JCR_INSTALLER, "JCR Installer");
	}

	private void checkComponentActive(String name, String prettyName) {
		Component component = this.retrieveComponentByName(name);
		Assert.assertNotNull(String.format("%s component is null.", prettyName), component);
		Assert.assertEquals(
				String.format("%s component is not active.", prettyName),
				Component.STATE_ACTIVE,
				component.getState()
				);
	}

	private Component retrieveComponentByName(String name) {
		Component[] components = this.scrService.getComponents();
		for (Component component : components) {
			if (component.getName().equals(name)) {
				return component;
			}
		}
		return null;
	}
}
