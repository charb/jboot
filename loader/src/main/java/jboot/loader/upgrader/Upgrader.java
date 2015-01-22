package jboot.loader.upgrader;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.boot.node.DefaultModelLoader;
import jboot.loader.boot.repository.DefaultModelRepositoryLayout;
import jboot.loader.boot.resolver.DefaultProjectResolver;
import jboot.loader.boot.resolver.ExceptionCollector;
import jboot.loader.boot.resolver.IProjectResolver;
import jboot.loader.boot.resolver.ModelNodeResult;
import jboot.repository.client.IRepository;
import jboot.repository.client.builder.DefaultProjectInfoResolver;
import jboot.repository.client.builder.IProjectInfoResolver;
import jboot.repository.client.checksum.CRC32Calculator;
import jboot.repository.client.checksum.IChecksumCalculator;
import jboot.repository.client.info.ModelNodeInfoResult;
import jboot.repository.client.info.model.ModelInfo;
import jboot.repository.client.info.model.ModelNodeInfos;

public class Upgrader {
	private static final Logger log = Logger.getLogger("jboot.upgrader");
	private static final String UPGRADE_LCK = "upgrade.lck";

	private IRepository remoteRepository;
	private DefaultModelRepositoryLayout localRepository;
	private ModelNodeResult modelNodeResult;
	private File repositoryCacheInfoFile;
	private RepositoryCacheInfo repositoryCacheInfo;
	private ModelNodeInfoResult localModelNodeInfoResult;
	private ModelNodeInfoResult remoteModelNodeInfoResult;
	private IProjectInfoResolver projectInfoResolver;
	private List<ModelInfo> rootModels;
	private IUpgraderListener upgraderListener;
	private UpgraderVisitor upgraderVisitor;
	private File lockFile;
	private FileChannel channel;
	private FileLock lock;
	private long sleepTimeBetweenLockRetries = 10000;

	public Upgrader(IRepository remoteRepository, DefaultModelRepositoryLayout localRespository) throws Exception {
		this.remoteRepository = remoteRepository;
		this.localRepository = localRespository;
		this.modelNodeResult = new ModelNodeResult();
		this.localModelNodeInfoResult = new ModelNodeInfoResult();
		this.remoteModelNodeInfoResult = new ModelNodeInfoResult();
		IProjectResolver projectResolver = new DefaultProjectResolver(modelNodeResult, localRespository, new DefaultModelLoader());
		this.projectInfoResolver = new DefaultProjectInfoResolver(projectResolver, modelNodeResult, localModelNodeInfoResult, false); //don't fill checksums. they will be computed lazily in the upgraderVisitor.
		this.rootModels = new ArrayList<ModelInfo>();
		this.upgraderListener = new UpgraderLogger();
		repositoryCacheInfoFile = new File(localRepository.getRepositoryRootPath() + File.separator + RepositoryCacheInfo.REPO_INFOS_XML);
		repositoryCacheInfo = new RepositoryCacheInfo();
		IChecksumCalculator checksumCalculator = new CRC32Calculator();
		this.upgraderVisitor = new UpgraderVisitor(repositoryCacheInfo, remoteRepository, localRespository, remoteModelNodeInfoResult, localModelNodeInfoResult, modelNodeResult, projectInfoResolver, upgraderListener, checksumCalculator);
		init();
	}

	private void init() throws Exception {
		File localRepositoryFile = new File(localRepository.getRepositoryRootPath());
		if (!localRepositoryFile.exists()) {
			localRepositoryFile.mkdirs();
		}

		this.lockFile = new File(localRepository.getRepositoryRootPath() + File.separator + UPGRADE_LCK);
		if (!lockFile.exists()) {
			lockFile.createNewFile();
		}
		channel = new RandomAccessFile(lockFile, "rw").getChannel();
	}

	public void addProject(String groupId, String artifactId, String version) throws Exception {
		rootModels.add(new ModelInfo(groupId, artifactId, version));
	}

	public void upgrade() throws Exception {
		lock();
        try {
			upgraderListener.upgradeStarted();
			loadRepositoryCacheInfo();
			for (ModelInfo modelInfo : rootModels) {
				ExceptionCollector collector = projectInfoResolver.resolve(modelInfo.getGroupId(), modelInfo.getArtifactId(), modelInfo.getVersion());
				collector.log();
			}
			for (ModelInfo modelInfo : rootModels) {
				ModelNodeInfos modelNodeInfos = remoteRepository.getModelNodeInfos(modelInfo.getGroupId(), modelInfo.getArtifactId(), modelInfo.getVersion());
				remoteModelNodeInfoResult.addModelNodeInfos(modelNodeInfos.getModelNodeInfos());
				upgraderVisitor.upgrade(modelInfo.getId());
			}
			saveRepositoryCacheInfo();
			upgraderListener.upgradeFinished();
        } finally {
        	unlock();
        }
	}

	private void loadRepositoryCacheInfo() throws Exception {
		if (log.isLoggable(Level.INFO)) {
			log.info("Loading repository status info from: " + repositoryCacheInfoFile.getAbsolutePath());
		}
		repositoryCacheInfo.read(repositoryCacheInfoFile);
	}

	private void saveRepositoryCacheInfo() throws Exception {
		if (log.isLoggable(Level.INFO)) {
			log.info("Saving repository status info to: " + repositoryCacheInfoFile.getAbsolutePath());
		}
		repositoryCacheInfo.save(repositoryCacheInfoFile);
	}

	private void lock() throws Exception {
		while (true) {
			try {
				upgraderListener.lockingFile(lockFile);
				lock = channel.tryLock();
				upgraderListener.lockSuccessful(lockFile);
				break;
			} catch (OverlappingFileLockException e) {
				upgraderListener.fileAlreadyLocked(lockFile);
				Thread.sleep(sleepTimeBetweenLockRetries);
			}
		}
	}

	private void unlock() throws Exception {
		if (lock != null) {
			lock.release();
			upgraderListener.fileUnlocked(lockFile);
		}
	}

	public void clear() {
		upgraderVisitor.clearVisited();
		projectInfoResolver.clear();
		rootModels.clear();
		modelNodeResult.clear();
		localModelNodeInfoResult.clear();
		remoteModelNodeInfoResult.clear();
		repositoryCacheInfo.clear();
	}

	@Override
	protected void finalize() throws Throwable {
		channel.close();
		super.finalize();
	}
}
