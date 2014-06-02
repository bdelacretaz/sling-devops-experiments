package org.apache.sling.devops.orchestrator.git;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class RemoteGitFileMonitor extends GitFileMonitor {

	public RemoteGitFileMonitor(final String repoUrl, final String filePath) throws GitAPIException, IOException {
		this(repoUrl, filePath, DEFAULT_PERIOD, DEFAULT_PERIOD_UNIT);
	}

	public RemoteGitFileMonitor(final String repoUrl, final String localParentPath, final String filePath) throws GitAPIException, IOException {
		this(repoUrl, localParentPath, filePath, DEFAULT_PERIOD, DEFAULT_PERIOD_UNIT);
	}

	public RemoteGitFileMonitor(final String repoUrl, final String filePath, final int period, final TimeUnit periodUnit) throws GitAPIException, IOException {
		this(repoUrl, Files.createTempDirectory("gitmon").toAbsolutePath().toString(), filePath, period, periodUnit);
	}

	public RemoteGitFileMonitor(final String repoUrl, final String localPath, final String filePath, final int period, final TimeUnit periodUnit) throws GitAPIException, IOException {
		super(Git.cloneRepository().setURI(repoUrl).setBare(true).setDirectory(new File(localPath)).call(), filePath, period, periodUnit);
		logger.info("Monitoring file {} in remote Git repository {} (local clone at {}).", filePath, repoUrl, localPath);
	}

	@Override
	public boolean isRemote() {
		return true;
	}

	@Override
	public synchronized void close() {
		super.close();
		delete(this.getLocalRepoPath());
	}

	private static void delete(final String path) {
		delete(new File(path));
	}

	private static void delete(final File path) {
		if (path.isDirectory()) {
			for (final File file : path.listFiles()) delete(file);
		}
		path.delete();
	}

	public static void main(String[] args) throws GitAPIException, IOException {
		final String REPO_URI = "https://github.com/ArtyomStetsenko/sling-devops-experiments.git";
		try (GitFileMonitor gitTest = new RemoteGitFileMonitor(REPO_URI, "resources/test1.esp", 10, TimeUnit.SECONDS)) {
			gitTest.addListener(new GitFileMonitor.GitFileListener() {
				@Override
				public void onModified(long time, ByteBuffer content) {
					System.out.println(new Date(time));
					while (content.hasRemaining()) {
						System.out.print((char)content.get());
					}
				}
			});
			gitTest.start();
			System.in.read();
		}
	}
}
