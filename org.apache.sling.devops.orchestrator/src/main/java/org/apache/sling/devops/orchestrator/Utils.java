package org.apache.sling.devops.orchestrator;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Utils {

	public static List<String> readStream(final InputStream stream) {
		final List<String> lines = new LinkedList<>();
		try (Scanner streamScanner = new Scanner(stream)) {
			while (streamScanner.hasNextLine()) lines.add(streamScanner.nextLine());
		}
		return lines;
	}

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
