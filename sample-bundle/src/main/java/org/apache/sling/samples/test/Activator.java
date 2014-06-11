package org.apache.sling.samples.test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	private static final String SOURCE = "/sample-test";
	private static final String DEST = "/sling-cfg"; // real dest will be /sling-cfg/<config>

	@Override
	public void start(final BundleContext context) throws Exception {
		final String config = context.getProperty("sling.devops.config");
		if (config != null) {
			ResourceResolver adminResolver = null;
			try {
				final ServiceReference ref = context.getServiceReference("org.apache.sling.api.resource.ResourceResolverFactory");
				final ResourceResolverFactory resourceResolverFactory = (ResourceResolverFactory) context.getService(ref);
				adminResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
				final Session adminSession = adminResolver.adaptTo(Session.class);

				// navigate to source node
				final String[] sourceParts = SOURCE.split("/");
				Node sourceNode = adminSession.getRootNode();
				for (int i = 1; i < sourceParts.length; i++) {
					sourceNode = sourceNode.getNode(sourceParts[i]);
				}

				// create destination node
				final String dest = DEST + '/' + config + '/';
				final String[] destParts = dest.split("/");
				Node destNode = adminSession.getRootNode();
				for (int i = 1; i < destParts.length; i++) {
					if (!destNode.hasNode(destParts[i])) destNode.addNode(destParts[i], "sling:Folder");
					destNode = destNode.getNode(destParts[i]);
				}

				// move each child of source node to destination node
				for (final NodeIterator it = sourceNode.getNodes(); it.hasNext(); ) {
					final Node node = it.nextNode();
					logger.info("Node {} will be moved to {}", node.getPath(), destNode.getPath());
					moveNode(adminSession, node, destNode);
				}
				sourceNode.remove();

				adminSession.save();
			} catch (final Exception e) {
				logger.error("Failure.", e);
			} finally {
				if (adminResolver != null) {
					adminResolver.close();
				}
			}
		} else logger.info("Config is null, not moving anything.");
	}

	private static void moveNode(final Session session, final Node src, final Node dest) throws Exception {
		// base case: src is a leaf or dest does not contain a node with src's name
		if (!src.hasNodes() || !dest.hasNode(src.getName())) {
			if (!dest.hasNode(src.getName())) { // leaf node: no overwrite
				logger.info("Moving node {} inside {}", src.getPath(), dest.getPath());
				session.move(src.getPath(), dest.getPath() + '/' + src.getName());
			} else logger.info(
						"Not moving leaf node {} inside {}: node with the same name already exists",
						src.getPath(),
						dest.getPath()
						);
		} else {
			final Node childDest = dest.getNode(src.getName());
			for (final NodeIterator it = src.getNodes(); it.hasNext(); ) {
				moveNode(session, it.nextNode(), childDest);
			}
			src.remove();
		}
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		// TODO Auto-generated method stub
	}
}
