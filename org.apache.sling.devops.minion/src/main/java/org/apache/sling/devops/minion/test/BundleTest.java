package org.apache.sling.devops.minion.test;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith(SlingAnnotationsTestRunner.class)
public class BundleTest {

	@TestReference
	private BundleContext bundleContext;

	@Test
	public void checkBundleContext() {
		Assert.assertNotNull("Bundle context is null.", this.bundleContext);
	}

	@Test
	public void checkBundlesActive() {
		for (Bundle bundle : this.bundleContext.getBundles()) {

			// Only if not fragment
			if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				Assert.assertEquals(
						String.format("Bundle %s is not active.", bundle),
						Bundle.ACTIVE,
						bundle.getState()
						);
			}
		}
	}

	@Test
	public void checkFragmentsResolved() {
		for (Bundle bundle : this.bundleContext.getBundles()) {

			// Only if fragment
			if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
				Assert.assertEquals(
						String.format("Bundle fragment %s is not resolved.", bundle),
						Bundle.RESOLVED,
						bundle.getState()
						);
			}
		}
	}
}
