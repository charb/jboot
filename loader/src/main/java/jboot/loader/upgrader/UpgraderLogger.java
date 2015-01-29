package jboot.loader.upgrader;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.log.LineReplaceLogFormatter;

public class UpgraderLogger implements IUpgraderListener {
	private static Logger log = Logger.getLogger("jboot.upgrader");
	private static LineReplaceLogFormatter lineReplaceLogFormatter = new LineReplaceLogFormatter();
	private Formatter cachedFormatter;

	public UpgraderLogger() {
	}

	private Handler getRootLoggerConsoleHandler() {
		Logger rootLogger = Logger.getLogger("");
		for (Handler handler : rootLogger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				return handler;
			}
		}
		return null;
	}

	@Override
	public void downloadFailed(String id, File file) {
		if (log.isLoggable(Level.WARNING)) {
			Handler handler = getRootLoggerConsoleHandler();
			if (handler != null) {
				handler.setFormatter(cachedFormatter);
			}
			log.warning("Download failed [artifact=" + id + ", file=" + file.getName() + "]");
		}
	}

	@Override
	public void downloadFinishedSuccessfully(String id, File file, long totalLength) {
		if (log.isLoggable(Level.INFO)) {
			Handler handler = getRootLoggerConsoleHandler();
			if (handler != null) {
				handler.setFormatter(cachedFormatter);
			}
			log.info("Downloaded [artifact=" + id + ", file=" + file.getName() + "] ... [" + file.length() + "/" + totalLength + " bytes]");
		}
	}

	@Override
	public void downloadInProgress(String id, File file, long downloadLength, long totalLength) {
		if (log.isLoggable(Level.INFO)) {
			long percentileDone = (downloadLength * 100) / (totalLength != 0 ? totalLength : 1);
			log.info("[" + downloadLength + "/" + totalLength + " bytes] [" + percentileDone + "%].");
		}
	}

	@Override
	public void downloadStarted(String id, File file, long totalLength) {
		if (log.isLoggable(Level.INFO)) {
			log.info("Downloading [artifact=" + id + ", file=" + file.getName() + "] ... [" + totalLength + " bytes]");
			Handler handler = getRootLoggerConsoleHandler();
			if (handler != null) {
				cachedFormatter = handler.getFormatter();
				handler.setFormatter(lineReplaceLogFormatter);
			}
		}
	}

	@Override
	public void upgradeFinished() {
		if (log.isLoggable(Level.INFO)) {
			log.info("Upgrade finished successfully.");
		}
	}

	@Override
	public void upgradeStarted() {
		if (log.isLoggable(Level.INFO)) {
			log.info("Upgrade started.");
		}
	}

	@Override
	public void lockingFile(File file) {
		if (log.isLoggable(Level.INFO)) {
			log.info("Locking file (" + file.getAbsolutePath() + ").");
		}
	}

	@Override
	public void fileUnlocked(File file) {
		if (log.isLoggable(Level.INFO)) {
			log.info("Unlocking file (" + file.getAbsolutePath() + ").");
		}
	}

	@Override
	public void fileAlreadyLocked(File file) {
		if (log.isLoggable(Level.INFO)) {
			log.info("File already locked, retrying ... (" + file.getAbsolutePath() + ")");
		}
	}

	@Override
	public void lockSuccessful(File file) {
		if (log.isLoggable(Level.INFO)) {
			log.info("File locked successfully (" + file.getAbsolutePath() + ").");
		}
	}
}
