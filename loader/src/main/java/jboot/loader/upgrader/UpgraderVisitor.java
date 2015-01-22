package jboot.loader.upgrader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.node.ModelNodeArtifact;
import jboot.loader.boot.node.resource.FileResource;
import jboot.loader.boot.repository.DefaultModelRepositoryLayout;
import jboot.loader.boot.resolver.ExceptionCollector;
import jboot.loader.boot.resolver.ModelNodeResult;
import jboot.repository.client.IRepository;
import jboot.repository.client.builder.IProjectInfoResolver;
import jboot.repository.client.checksum.IChecksumCalculator;
import jboot.repository.client.info.ModelNodeInfoResult;
import jboot.repository.client.info.model.ArtifactInfo;
import jboot.repository.client.info.model.ModelNodeInfo;
import jboot.repository.client.info.visitor.AbstractNodeInfoVisitor;

public class UpgraderVisitor extends AbstractNodeInfoVisitor {
	private static final Logger log = Logger.getLogger("jboot.upgrader");
	private RepositoryCacheInfo repositoryCacheInfo;
	private IRepository remoteRepository;
	private DefaultModelRepositoryLayout localRepository;
	private IProjectInfoResolver projectInfoResolver;
	private ModelNodeInfoResult remoteModelNodeInfoResult;
	private ModelNodeInfoResult localModelNodeInfoResult;
	private ModelNodeResult modelNodeResult;
	private IUpgraderListener upgraderListener;
	private IChecksumCalculator checksumCalculator;

	public UpgraderVisitor(RepositoryCacheInfo repositoryCacheInfo, IRepository remoteRepository, DefaultModelRepositoryLayout localRepository, ModelNodeInfoResult remoteModelNodeInfoResult, ModelNodeInfoResult localModelNodeInfoResult, ModelNodeResult modelNodeResult, IProjectInfoResolver projectInfoResolver, IUpgraderListener upgraderListener, IChecksumCalculator checksumCalculator) throws Exception {
		super(remoteModelNodeInfoResult); //visit by traversing the remote ModelNodeInfoResult
		this.repositoryCacheInfo = repositoryCacheInfo;
		this.remoteRepository = remoteRepository;
		this.localRepository = localRepository;
		this.remoteModelNodeInfoResult = remoteModelNodeInfoResult;
		this.localModelNodeInfoResult = localModelNodeInfoResult;
		this.modelNodeResult = modelNodeResult;
		this.projectInfoResolver = projectInfoResolver;
		this.upgraderListener = upgraderListener;
		this.checksumCalculator = checksumCalculator;
	}

	public void upgrade(String id) throws Exception {
		ModelNodeInfo referenceModelNodeInfo = remoteModelNodeInfoResult.getModelNodeInfo(id);
		if (referenceModelNodeInfo != null) {
			visit(referenceModelNodeInfo);
		}
	}

	public final void startVisiting(ModelNodeInfo referenceModelNodeInfo) throws Exception {
		if (referenceModelNodeInfo == null) {
			throw new IllegalStateException("Encountered a null ModelNodeInfo while traversing remote models.");
		}
		ModelNodeInfo cachedModelNodeInfo = repositoryCacheInfo.getModelNodeInfo(referenceModelNodeInfo.getId());
		ModelNodeInfo localModelNodeInfo = localModelNodeInfoResult.getModelNodeInfo(referenceModelNodeInfo.getId());
		if (cachedModelNodeInfo == null && localModelNodeInfo != null) {
			fillChecksums(localModelNodeInfo); //compute checksums for pom/artifacts that do not have a cached ModelNodeInfo.
			repositoryCacheInfo.addModelNodeInfo(localModelNodeInfo);
			cachedModelNodeInfo = localModelNodeInfo;
		} else if (localModelNodeInfo == null) {
			cachedModelNodeInfo = null;
		}
		compareModelNodeInfo(referenceModelNodeInfo, cachedModelNodeInfo);
	}

	private void compareModelNodeInfo(ModelNodeInfo referenceModelNodeInfo, ModelNodeInfo cachedModelNodeInfo) throws Exception {
		//here we should be sure that the reference and cached models have the same gid:aid:ver.
		if (cachedModelNodeInfo != null && !referenceModelNodeInfo.getId().equals(cachedModelNodeInfo.getId())) {
			throw new IllegalStateException("Cannot compare models having different ids.");
		}

		if (cachedModelNodeInfo == null) {
			if (log.isLoggable(Level.INFO)) {
				log.info("Local model does not exist: " + referenceModelNodeInfo);
				log.info("Fetching pom and artifacts for: " + referenceModelNodeInfo.getId());
			}
			fetchModelNodeInto(referenceModelNodeInfo);
		} else {
			comparePom(referenceModelNodeInfo, cachedModelNodeInfo);
			compareArtifacts(referenceModelNodeInfo, cachedModelNodeInfo);
		}

		repositoryCacheInfo.addModelNodeInfo(referenceModelNodeInfo);
	}

	private void compareArtifacts(ModelNodeInfo referenceModelNodeInfo, ModelNodeInfo cachedModelNodeInfo) throws Exception {
		Map<String, ArtifactInfo> cachedArtifacts = new HashMap<String, ArtifactInfo>();
		for (ArtifactInfo artifact : cachedModelNodeInfo.getArtifacts()) {
			cachedArtifacts.put(artifact.getClassifier() + ":" + artifact.getType(), artifact);  //TODO: we can't have multiple artifacts with same classifier but different types. just use classifier as key.
		}
		for (ArtifactInfo referenceArtifact : referenceModelNodeInfo.getArtifacts()) {
			ArtifactInfo cachedArtifact = cachedArtifacts.remove(referenceArtifact.getClassifier() + ":" + referenceArtifact.getType());
			if (cachedArtifact == null || !cachedArtifact.getChecksum().equals(referenceArtifact.getChecksum())) {
				if (log.isLoggable(Level.INFO)) {
					log.info("Artifacts different: " + (cachedArtifact==null?"local artifact does not exist. ":"local artifact: " + cachedArtifact) + " remote artifact: " + referenceArtifact);
				}
				fetchArtifact(referenceArtifact);
			}
		}
		//The remaining ArtifactInfos in the map are the extra ones that are not defined in the referenceModelNodeInfo.
		if (log.isLoggable(Level.INFO)) {
			for (ArtifactInfo excessArtifact : cachedArtifacts.values()) {
				log.info("Excess local artifact found: " + excessArtifact);
			}
		}
	}

	private void comparePom(ModelNodeInfo referenceModelNodeInfo, ModelNodeInfo cachedModelNodeInfo) throws Exception {
		if (!cachedModelNodeInfo.getPomChecksum().equals(referenceModelNodeInfo.getPomChecksum())) {
			if (log.isLoggable(Level.INFO)) {
				log.info("Poms different for model: " + referenceModelNodeInfo);
				log.info("Fetching pom: " + referenceModelNodeInfo.getId());
			}
			fetchPom(referenceModelNodeInfo);
		}
	}

	private void fillChecksums(ModelNodeInfo modelNodeInfo) throws Exception {
		getChecksum(modelNodeInfo);
		for (ArtifactInfo artifactInfo : modelNodeInfo.getArtifacts()) {
			getChecksum(artifactInfo);
		}
	}

	private String getChecksum(ModelNodeInfo modelNodeInfo) throws Exception {
		if (modelNodeInfo.getPomChecksum() != null) {
			return modelNodeInfo.getPomChecksum();
		}
		ModelNode modelNode = modelNodeResult.getModelNode(modelNodeInfo.getId());
		String checksum = checksumCalculator.getChecksum(modelNode.getPom().getFile());
		modelNodeInfo.setPomChecksum(checksum);
		return checksum;
	}

	private String getChecksum(ArtifactInfo artifactInfo) throws Exception {
		if (artifactInfo.getChecksum() != null) {
			return artifactInfo.getChecksum();
		}
		ModelNode modelNode = modelNodeResult.getModelNode(artifactInfo.getGroupId() + ":" + artifactInfo.getArtifactId() + ":" + artifactInfo.getVersion());
		ModelNodeArtifact modelNodeArtifact = modelNode.getArtifact(artifactInfo.getClassifier());
		FileResource fileResource = (FileResource) modelNodeArtifact.getResource();
		String checksum = checksumCalculator.getChecksum(fileResource.getFile());
		artifactInfo.setChecksum(checksum);
		return checksum;
	}

	private void fetchModelNodeInto(ModelNodeInfo modelNodeInfo) throws Exception {
		//fetch pom
		fetchPom(modelNodeInfo);

		//fetch all artifacts
		for (ArtifactInfo artifact : modelNodeInfo.getArtifacts()) {
			fetchArtifact(artifact);
		}

		//resolve
		modelNodeResult.removeMissingModel(modelNodeInfo.getGroupId(), modelNodeInfo.getArtifactId(), modelNodeInfo.getVersion());
		ExceptionCollector collector = projectInfoResolver.resolve(modelNodeInfo.getGroupId(), modelNodeInfo.getArtifactId(), modelNodeInfo.getVersion());
		collector.log();
	}

	private void fetchArtifact(ArtifactInfo artifact) throws Exception {
		InputStream in = remoteRepository.getArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier(), artifact.getType());
		String artifactPath = localRepository.getArtifactPath(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier(), artifact.getType());
		persistFile(artifact.getId(), artifactPath, in, artifact.getLength());
		File artifactFile = new File(artifactPath);
		ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(artifactFile);
        } catch (Throwable e) {
            log.log(Level.ALL, "", e);
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
	}

	private void fetchPom(ModelNodeInfo modelNodeInfo) throws Exception {
		InputStream in = remoteRepository.getPom(modelNodeInfo.getGroupId(), modelNodeInfo.getArtifactId(), modelNodeInfo.getVersion());
		String pomPath = localRepository.getPomPath(modelNodeInfo.getGroupId(), modelNodeInfo.getArtifactId(), modelNodeInfo.getVersion());
		persistFile(modelNodeInfo.getId(), pomPath, in, modelNodeInfo.getPomLength());
	}

	private void persistFile(String modelId, String filePath, InputStream in, long length) throws Exception {
		OutputStream out = null;
		File file = new File(filePath);
		file.getParentFile().mkdirs();
		file.createNewFile();
		try {
            upgraderListener.downloadStarted(modelId, file, length);
			out = new BufferedOutputStream(new FileOutputStream(file));
			byte[] buf = new byte[1024];
			int len;
			long size = 0;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
				size += len;
				upgraderListener.downloadInProgress(modelId, file, size, length);
			}
			out.flush();
			upgraderListener.downloadFinishedSuccessfully(modelId, file, length);
		} catch (Exception ex) {
			upgraderListener.downloadFailed(modelId, file);
			throw ex;
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}
