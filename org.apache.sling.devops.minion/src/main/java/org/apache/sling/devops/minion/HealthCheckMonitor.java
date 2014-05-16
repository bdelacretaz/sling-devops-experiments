package org.apache.sling.devops.minion;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckMonitor implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(HealthCheckMonitor.class);

	private final ScheduledExecutorService scheduledExecutorService;
	private final HealthCheckExecutor healthCheckExecutor;
	private final Runnable healthCheckExecutorRunner;
	private final List<HealthCheckListener> listeners;
	private ScheduledFuture<?> healthCheckExecutorRunnerFuture;
	private int count;
	private boolean isOk;

	public HealthCheckMonitor(final HealthCheckExecutor healthCheckExecutor, final String... tags) {
		this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		this.healthCheckExecutor = healthCheckExecutor;
		this.healthCheckExecutorRunner = new Runnable() {
			@Override
			public void run() {
				HealthCheckMonitor.this.process(
						HealthCheckMonitor.this.healthCheckExecutor.execute(tags));
			}
		};
		this.listeners = new LinkedList<>();
	}

	public synchronized void start() {
		this.count = 0;
		this.isOk = false;
		this.scheduledExecutorService.execute(this.healthCheckExecutorRunner);
	}

	public synchronized void addListener(final HealthCheckListener listener) {
		if (this.isOk) listener.onOk();
		else this.listeners.add(listener);
	}

	private synchronized void process(final List<HealthCheckExecutionResult> results) {
		if (!this.isOk) {
			boolean allOk = results.size() > 0;
			for (final HealthCheckExecutionResult result : results) {
				if (!result.getHealthCheckResult().isOk()) {
					allOk = false;
					break;
				}
			}
			if (allOk) {
				this.isOk = true;
				logger.info("Health checks succeeded.");
			} else {
				final int waitingTime = 1 << this.count++; // 1, 2, 4, 8, 16... = exponential backoff
				this.healthCheckExecutorRunnerFuture = this.scheduledExecutorService.schedule(
						this.healthCheckExecutorRunner,
						waitingTime,
						TimeUnit.SECONDS
						);
				logger.warn("Health checks failed, retrying in {} seconds...", waitingTime);
			}
			for (final HealthCheckListener listener : this.listeners) {
				if (allOk) listener.onOk();
				else listener.onFail();
			}
		}
	}

	@Override
	public synchronized void close() {
		this.listeners.clear();
		if (this.healthCheckExecutorRunnerFuture != null) {
			this.healthCheckExecutorRunnerFuture.cancel(false);
		}
		this.scheduledExecutorService.shutdown();
	}

	public interface HealthCheckListener {
		public void onOk();
		public void onFail();
	}
}
