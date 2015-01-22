package jboot.loader.upgrader;

import java.io.File;

public interface IUpgraderListener {
	public void upgradeStarted();

	public void downloadStarted(String id, File file, long totalLength);

	public void downloadInProgress(String id, File file, long downloadLength, long totalLength);

	public void downloadFinishedSuccessfully(String id, File file, long totalLength);

	public void downloadFailed(String id, File file);

	public void upgradeFinished();

	public void lockingFile(File file);

	public void fileUnlocked(File file);

	public void fileAlreadyLocked(File file);

	public void lockSuccessful(File file);
}
