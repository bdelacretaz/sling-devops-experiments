package org.apache.sling.devops.minion;

import java.io.IOException;
import java.util.Set;

import org.apache.sling.devops.Instance;
import org.apache.sling.devops.zookeeper.ZooKeeperConnector;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperInstanceAnnouncer implements InstanceAnnouncer {

	private static final Logger logger = LoggerFactory.getLogger(ZooKeeperInstanceAnnouncer.class);

	private ZooKeeperConnector zkConnector;

	public ZooKeeperInstanceAnnouncer(String connectionString) throws IOException {
		this.zkConnector = new ZooKeeperConnector(connectionString, new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				if (event.getType() != Event.EventType.None) {
					// our node changed, maybe another instance has the same ID?
					logger.warn("Node was changed, closing ZooKeeper connection.");
					ZooKeeperInstanceAnnouncer.this.zkConnector.close();
				}
			}
		});
	}

	@Override
	public void announce(Instance instance) {
		String config = instance.getConfig();
		Set<String> endpoints = instance.getEndpoints();
		logger.info("Announcing config={}, endpoints={}", config, endpoints);

		StringBuilder endpointString = new StringBuilder();
		String prefix = "";
		for (String endpoint : endpoints) {
			endpointString.append(prefix);
			endpointString.append(endpoint);
			prefix = ",";
		}
		final String zkPath = "/" + instance.getId();

		// Delete existing node, if any
		this.zkConnector.getZooKeeper().delete(
				zkPath,
				-1,
				new AsyncCallback.VoidCallback() {
					@Override
					public void processResult(int code, String path, Object ctx) {
						// either deleted or doesn't exist, doesn't matter
					}
				},
				null
				);

		// Create node with info
		this.zkConnector.getZooKeeper().create(
				zkPath,
				String.format("config=%s;endpoints=%s", config, endpointString).getBytes(),
				ZooDefs.Ids.OPEN_ACL_UNSAFE,
				CreateMode.EPHEMERAL,
				new AsyncCallback.StringCallback() {
					@Override
					public void processResult(int code, String path, Object ctx, String name) {
						switch (Code.get(code)) {
						case NODEEXISTS:
							logger.warn("Node exists, could not create.");
							ZooKeeperInstanceAnnouncer.this.zkConnector.close();
							break;
						case NONODE:
						case OK:
							// set a watch on node
							ZooKeeperInstanceAnnouncer.this.zkConnector.getZooKeeper().exists(
									zkPath,
									true,
									new AsyncCallback.StatCallback() {
										@Override
										public void processResult(int code, String path, Object ctx, Stat stat) {
											switch (Code.get(code)) {
											case NONODE:
												logger.warn("Node doesn't exist, could not watch.");
												ZooKeeperInstanceAnnouncer.this.zkConnector.close();
												break;
											case OK:
												logger.info("Node created.");
												break;
											default: break;
											}
										}
									},
									null
									);
							break;
						default: break;
						}
					}
				},
				null // ctx
				);
	}

	@Override
	public void close() {
		this.zkConnector.close();
	}
}
