package org.apache.sling.devops.orchestrator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModProxyConfigTransitioner implements ConfigTransitioner {

	private static final Logger logger = LoggerFactory.getLogger(ModProxyConfigTransitioner.class);

	private final String proxyExecutable;
	private final String filePath;
	private final String sudoPassword;

	public ModProxyConfigTransitioner(String proxyExecutable, String filePath, String sudoPassword) {
		this.proxyExecutable = proxyExecutable;
		this.filePath = filePath;
		this.sudoPassword = sudoPassword;
	}

	@Override
	public void transition(String config, Set<String> instances) throws IOException, InterruptedException {

		// Update config file
		try (PrintWriter writer = new PrintWriter(this.filePath)) {
			for (String instance : instances) {
				writer.println(String.format(
						"BalancerMember %s route=%s",
						instance.charAt(instance.length() - 1) == '/' ? // mod_proxy_balancer complains about trailing slashes
								instance.substring(0, instance.length() - 1) :
									instance,
						instance.hashCode()
						));
			}
		}

		// Relaunch proxy
		Process process = new ProcessBuilder("sudo", "-k", "-S", this.proxyExecutable, "graceful").start();
		try (PrintWriter pw = new PrintWriter(process.getOutputStream())) {
			pw.println(this.sudoPassword);
		}
		int exitValue = process.waitFor();
		if (exitValue != 0) {
			try (Scanner errorStream = new Scanner(process.getErrorStream())) {
				logger.error("Proxy restart exited with value {}, see the following output.", exitValue);
				while (errorStream.hasNextLine()) logger.error(errorStream.nextLine());
			}
		} else {
			logger.info("Proxy restarted.");
		}
	}
}
