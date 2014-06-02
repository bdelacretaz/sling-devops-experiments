package org.apache.sling.devops.orchestrator.git;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GitFileMonitor implements Closeable {

	protected static final Logger logger = LoggerFactory.getLogger(GitFileMonitor.class);

	public static final int DEFAULT_PERIOD = 1;
	public static final TimeUnit DEFAULT_PERIOD_UNIT = TimeUnit.MINUTES;

	private final Git git;
	private final Repository repo;
	private final RevWalk revWalk;
	private final PathFilter filePathFilter;
	private final int period;
	private final TimeUnit periodUnit;
	private final List<GitFileListener> listeners;
	private final ScheduledExecutorService scheduledExecutorService;
	private ScheduledFuture<?> scheduledFuture;
	private ObjectId headCommitId;
	private RevCommit latestFileCommit;
	private ByteBuffer latestFileContent = ByteBuffer.allocate(0);

	protected GitFileMonitor(final Git git, final String filePath, final int period, final TimeUnit periodUnit) {
		if (git == null) throw new IllegalArgumentException("Git is null.");
		if (filePath == null) throw new IllegalArgumentException("File path is null.");
		if (periodUnit == null) throw new IllegalArgumentException("Period unit is null.");

		this.git = git;
		this.filePathFilter = PathFilter.create(filePath);
		this.repo = this.git.getRepository();
		this.revWalk = new RevWalk(this.repo);
		this.revWalk.setTreeFilter(AndTreeFilter.create(this.filePathFilter, TreeFilter.ANY_DIFF));
		this.revWalk.sort(RevSort.COMMIT_TIME_DESC);
		this.period = period;
		this.periodUnit = periodUnit;
		this.listeners = new LinkedList<>();
		this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
	}

	public synchronized void start() {
		if (this.scheduledFuture == null) {
			this.scheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(
					new Runnable() {
						@Override
						public void run() {
							try {
								GitFileMonitor.this.checkForModification();
							} catch (Exception e) {
								logger.error("Something went wrong.", e);
							}
						}
					},
					0,
					this.period,
					this.periodUnit
					);
		}
	}

	public String getLocalRepoPath() {
		return this.repo.getDirectory().getAbsolutePath();
	}

	public synchronized ByteBuffer getLatestFileContent() {
		return this.latestFileContent.asReadOnlyBuffer();
	}

	public synchronized void addListener(final GitFileListener listener) {
		if (listener == null) throw new IllegalArgumentException("Listener is null.");
		this.listeners.add(listener);
	}

	@Override
	public synchronized void close() {
		this.git.close();
		if (this.scheduledFuture != null) {
			this.scheduledFuture.cancel(false);
		}
		this.scheduledExecutorService.shutdown();
	}

	public abstract boolean isRemote();

	private synchronized void checkForModification() throws GitAPIException, IOException {
		if (this.isRemote()) {
			this.git.fetch().call();
		}
		final ObjectId newHeadCommitId = this.repo.resolve(Constants.HEAD);
		if (newHeadCommitId != null && !newHeadCommitId.equals(this.headCommitId)) {
			logger.info("Detected new head commit {}.", newHeadCommitId.getName());
			this.headCommitId = newHeadCommitId;
			this.revWalk.reset();
			this.revWalk.markStart(this.revWalk.parseCommit(newHeadCommitId));
			final RevCommit newLatestFileCommit = this.revWalk.next();
			final long time;
			final ByteBuffer content;
			if (newLatestFileCommit == null) {
				logger.warn("Could not find file with path {}.", this.filePathFilter.getPath());
				time = 0;
				content = ByteBuffer.allocate(0);
			} else if (!newLatestFileCommit.equals(this.latestFileCommit)) {
				final TreeWalk treeWalk = new TreeWalk(this.repo);
				treeWalk.addTree(newLatestFileCommit.getTree());
				treeWalk.setRecursive(true);
				treeWalk.setFilter(this.filePathFilter);
				if (!treeWalk.next()) {
					logger.warn("File with path {} no longer exists.", this.filePathFilter.getPath());
					time = 0;
					content = ByteBuffer.allocate(0);
				} else {
					final ObjectId objectId = treeWalk.getObjectId(0);
					time = newLatestFileCommit.getAuthorIdent().getWhen().getTime();
					content = ByteBuffer.wrap(this.repo.open(objectId).getCachedBytes());
				}
			} else return;
			if (!this.latestFileContent.equals(content)) {
				logger.info("File changed.");
				for (final GitFileListener listener : this.listeners) {
					listener.onModified(time, content.asReadOnlyBuffer());
				}
				this.latestFileContent = content;
			}
			this.latestFileCommit = newLatestFileCommit;
		}
	}

	public interface GitFileListener {
		public void onModified(long time, ByteBuffer content);
	}
}
