package org.apache.sling.devops.orchestrator;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModProxyConfigTransitioner implements ConfigTransitioner {

	private static final Logger logger = LoggerFactory.getLogger(ModProxyConfigTransitioner.class);

	private final String proxyExecutable;
	private final String filePath;
	private final String sudoPassword;

	public ModProxyConfigTransitioner(String proxyExecutable, String filePath, String sudoPassword) throws IOException, InterruptedException {
		this.proxyExecutable = proxyExecutable;
		this.filePath = filePath;
		this.sudoPassword = sudoPassword;
		this.execProxyCommand("stop"); // ensure proxy is stopped initially
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
		logger.info("Proxy config {} rewritten for instances {}", this.filePath, instances);

		// Relaunch proxy
		this.execProxyCommand("graceful");
	}

	private void execProxyCommand(String command) throws IOException, InterruptedException {
		Process process = new ProcessBuilder("sudo", "-k", "-S", this.proxyExecutable, command).start();
		try (PrintWriter pw = new PrintWriter(process.getOutputStream())) {
			pw.println(this.sudoPassword);
		}

		// Read streams so that process does not block
		final List<String> output = Utils.readStream(process.getInputStream());
		final List<String> errors = Utils.readStream(process.getErrorStream());

		final int exitValue = process.waitFor();

		// Log output
		for (final String out : output) logger.info(out);

		// Log errors: ERROR level if exit code not 0, WARN level otherwise
		if (exitValue != 0) {
			for (final String error : errors) logger.error(error);
			logger.error("Proxy command \"{}\" exited with value {}.", command, exitValue);
		} else {
			for (final String error : errors) logger.warn(error);
			logger.info("Proxy command \"{}\" succeeded.", command);
		}
	}
}
