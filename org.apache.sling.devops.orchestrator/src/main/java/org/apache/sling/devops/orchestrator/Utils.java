package org.apache.sling.devops.orchestrator;

import java.io.File;

public class Utils {

	public static void delete(final String path) {
		delete(new File(path));
	}

	public static void delete(final File path) {
		if (path.isDirectory()) {
			for (final File file : path.listFiles()) delete(file);
		}
		path.delete();
	}
}
