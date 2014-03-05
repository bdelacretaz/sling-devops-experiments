package org.apache.sling.samples.configannouncer;

import java.io.IOException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.devops.zookeeper.ZooKeeperConnector;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service
public class ZooKeeperConfigAnnouncer extends ConfigAnnouncer {

	private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConfigAnnouncer.class);

	public static final String ZK_ROOT = "/sling"; // no hierarchy, top-level node name only

	private ZooKeeperConnector zkConnector;

	@Override
	public void announceConfig() {
		String config = this.getConfig();
		String endpoints = this.getEndpoints();
		logger.info("Announcing config={}, endpoints={}", config, endpoints);

		String zkPath = "/" + this.getSlingId();

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
				String.format("config=%s;endpoints=%s", config, endpoints).getBytes(),
				ZooDefs.Ids.OPEN_ACL_UNSAFE,
				CreateMode.EPHEMERAL,
				new AsyncCallback.StringCallback() {
					@Override
					public void processResult(int code, String path, Object ctx, String ret) {
						switch (Code.get(code)) {
						case NODEEXISTS:
							logger.warn("Node exists, could not create.");
							ZooKeeperConfigAnnouncer.this.closeZooKeeperConnector();
							break;
						case OK:
							logger.info("Node created.");
							break;
						default: break;
						}
					}
				},
				null // ctx
				);
	}

	@Activate
	protected void onActivate() throws IOException {
		String zkConnectionString = System.getProperty("zookeeper.connString"); // TODO
		if (zkConnectionString == null) zkConnectionString = "localhost:2181";
		this.zkConnector = new ZooKeeperConnector(
				zkConnectionString + ZK_ROOT,
				new Watcher() {
					@Override
					public void process(WatchedEvent event) {
						if (event.getType() != Event.EventType.None) {
							// our node changed, maybe another instance has the same ID?
							logger.warn("Node was changed, closing ZooKeeper connection.");
							ZooKeeperConfigAnnouncer.this.closeZooKeeperConnector();
						}
					}
				}
				);
	}

	@Deactivate
	protected void onDeactivate() {
		this.closeZooKeeperConnector();
	}

	private void closeZooKeeperConnector() {
		this.zkConnector.close();
	}
}
