package org.apache.sling.devops.orchestrator.git;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class LocalGitFileMonitor extends GitFileMonitor {

	public LocalGitFileMonitor(final String repoPath, final String filePath, final int period, final TimeUnit periodUnit) throws IOException {
		super(Git.open(new File(repoPath)), filePath, period, periodUnit);
		logger.info("Monitoring file {} in local Git repository {}.", filePath, repoPath);
	}

	public boolean isRemote() {
		return false;
	}

	public static void main(String[] args) throws URISyntaxException, GitAPIException, IOException {
		try (GitFileMonitor gitTest = new LocalGitFileMonitor("/Users/stetsenk/Documents/workspace/Test/blah", "coo2.txt", 10, TimeUnit.SECONDS)) {
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
