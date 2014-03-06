package org.apache.sling.devops.zookeeper;

import java.io.Closeable;
import java.io.IOException;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperConnector implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConnector.class);

	public static final String ZK_CONNECTION_STRING;
	static {
		String connectionString = System.getProperty("zookeeper.connString"); // TODO
		if (connectionString == null) connectionString = "localhost:2181";
		ZK_CONNECTION_STRING = connectionString;
	}
	public static final String ZK_ROOT = "/sling"; // no hierarchy, top-level node name only
	public static final int ZK_SESSION_TIMEOUT = 30 * 1000;

	private final ZooKeeper zooKeeper;

	public ZooKeeperConnector(Watcher watcher) throws IOException {
		this(ZK_CONNECTION_STRING, ZK_SESSION_TIMEOUT, watcher);
	}

	public ZooKeeperConnector(String connectionString, int sessionTimeout, Watcher watcher) throws IOException {
		this.zooKeeper = new ZooKeeper(
				connectionString + ZK_ROOT,
				sessionTimeout,
				watcher
				);

		// Create root node if doesn't exist
		this.zooKeeper.create(
				"/",
				null,
				ZooDefs.Ids.OPEN_ACL_UNSAFE,
				CreateMode.PERSISTENT,
				new AsyncCallback.StringCallback() {
					@Override
					public void processResult(int code, String path, Object ctx, String ret) {
						// either created or already exists, doesn't matter
						logger.info("ZooKeeper connection established.");
					}
				},
				null // ctx
				);
	}

	public ZooKeeper getZooKeeper() {
		return this.zooKeeper;
	}

	@Override
	public void close() {
		try {
			this.zooKeeper.close();
		} catch (InterruptedException e) {
			// empty
		}
		logger.info("ZooKeeper connection closed.");
	}
}
