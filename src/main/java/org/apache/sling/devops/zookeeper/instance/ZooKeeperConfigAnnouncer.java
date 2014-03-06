package org.apache.sling.devops.zookeeper.instance;

import java.io.IOException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.devops.instance.ConfigAnnouncer;
import org.apache.sling.devops.zookeeper.ZooKeeperConnector;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service
public class ZooKeeperConfigAnnouncer extends ConfigAnnouncer {

	private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConfigAnnouncer.class);

	private ZooKeeperConnector zkConnector;

	@Override
	public void announceConfig() {
		String config = this.getConfig();
		String endpoints = this.getEndpoints();
		logger.info("Announcing config={}, endpoints={}", config, endpoints);

		final String zkPath = "/" + this.getSlingId();

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
						case NONODE:
						case OK:
							// set a watch on node
							ZooKeeperConfigAnnouncer.this.zkConnector.getZooKeeper().exists(
									zkPath,
									true,
									new AsyncCallback.StatCallback() {
										@Override
										public void processResult(int code, String path, Object ctx, Stat ret) {
											switch (Code.get(code)) {
											case NONODE:
												logger.warn("Node doesn't exist, could not watch.");
												ZooKeeperConfigAnnouncer.this.closeZooKeeperConnector();
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

	@Activate
	protected void onActivate() throws IOException {
		this.zkConnector = new ZooKeeperConnector(new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				if (event.getType() != Event.EventType.None) {
					// our node changed, maybe another instance has the same ID?
					logger.warn("Node was changed, closing ZooKeeper connection.");
					ZooKeeperConfigAnnouncer.this.zkConnector.close();
				}
			}
		});
	}

	@Deactivate
	protected void onDeactivate() {
		this.closeZooKeeperConnector();
	}

	private void closeZooKeeperConnector() {
		this.zkConnector.close();
	}
}
