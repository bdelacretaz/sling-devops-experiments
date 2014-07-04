package org.apache.sling.devops.orchestrator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModProxyConfigTransitioner implements ConfigTransitioner {

	private static final Logger logger = LoggerFactory.getLogger(ModProxyConfigTransitioner.class);

	private final CommandLine baseCommandLine;
	private final String filePath;
	private final String sudoPassword;

	public ModProxyConfigTransitioner(String proxyExecutable, String filePath, String sudoPassword) throws IOException, InterruptedException {
		this.baseCommandLine = new CommandLine("sudo");
		this.baseCommandLine.addArgument("-k");
		this.baseCommandLine.addArgument("-S");
		this.baseCommandLine.addArgument(proxyExecutable);
		this.baseCommandLine.addArgument("${command}");
		this.filePath = filePath;
		this.sudoPassword = sudoPassword;
		this.execProxyCommand("stop"); // ensure proxy is stopped initially
	}

	@Override
	public void transition(String config, Set<String> endpoints) throws IOException, InterruptedException {

		// Update config file
		try (PrintWriter writer = new PrintWriter(this.filePath)) {
			for (String endpoint : endpoints) {
				writer.println(String.format(
						"BalancerMember %s route=%s",
						endpoint.charAt(endpoint.length() - 1) == '/' ? // mod_proxy_balancer complains about trailing slashes
								endpoint.substring(0, endpoint.length() - 1) :
									endpoint,
						endpoint.hashCode()
						));
			}
		}
		logger.info("Proxy config {} rewritten for endpoints {}", this.filePath, endpoints);

		// Relaunch proxy
		this.execProxyCommand("graceful");
	}

	@Override
	public void close() throws IOException {
		this.execProxyCommand("stop");
	}

	private void execProxyCommand(final String command) {

		final CommandLine commandLine = new CommandLine(this.baseCommandLine);

		// Prepare params
		final Map<String, Object> params = new HashMap<>();
		params.put("command", command);
		commandLine.setSubstitutionMap(params);

		// Run!
		final Executor executor = new DefaultExecutor();
		final List<String> errors = new LinkedList<>();
		executor.setStreamHandler(new PumpStreamHandler(
				new LogOutputStream() { // stdout: log as INFO
					@Override
					protected void processLine(final String line, final int level) {
						logger.info(line);
					}
				},
				new LogOutputStream() { // stderr: remember
					@Override
					protected void processLine(final String line, final int level) {
						errors.add(line);
					}
				},
				this.sudoPassword == null ? null : // stdin: feed password if available
					new ByteArrayInputStream(this.sudoPassword.getBytes(StandardCharsets.UTF_8))
				));
		
		try {
	        final int exitValue = executor.execute(commandLine);
	        
	        // Log errors: ERROR level if exit code not 0, WARN level otherwise
	        if (exitValue != 0) {
	            for (final String error : errors) logger.error(error);
	            logger.error("Proxy command \"{}\" exited with value {}.", command, exitValue);
	        } else {
	            for (final String error : errors) logger.warn(error);
	            logger.info("Proxy command \"{}\" succeeded.", command);
	        }
		} catch(IOException ioe) {
		    logger.error("Command execution failed :" + commandLine, ioe);
		}

	}
}
