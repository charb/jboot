package jboot.repository.client.info;

import jboot.loader.node.ModelNode;
import jboot.loader.node.ModelNodeArtifact;
import jboot.loader.node.ModelNodeDependency;
import jboot.loader.node.resource.FileResource;
import jboot.loader.resolver.ModelNodeResult;
import jboot.loader.visitor.AbstractNodeVisitor;
import jboot.repository.client.checksum.CRC32Calculator;
import jboot.repository.client.checksum.IChecksumCalculator;
import jboot.repository.client.info.model.ArtifactInfo;
import jboot.repository.client.info.model.DependencyInfo;
import jboot.repository.client.info.model.ModelNodeInfo;

public class ModelNodeInfoBuilderVisitor extends AbstractNodeVisitor {
	private ModelNodeInfoResult modelNodeInfoResult;
	private IChecksumCalculator checksumCalculator;

	public ModelNodeInfoBuilderVisitor(ModelNodeResult modelNodeResult, ModelNodeInfoResult modelNodeInfoResult) {
		this(modelNodeResult, modelNodeInfoResult, true);
	}

	public ModelNodeInfoBuilderVisitor(ModelNodeResult modelNodeResult, ModelNodeInfoResult modelNodeInfoResult, boolean fillChecksum) {
		super(modelNodeResult);
		this.modelNodeInfoResult = modelNodeInfoResult;
		if (fillChecksum == true) {
			this.checksumCalculator = new CRC32Calculator();
		}
	}

	@Override
	public void startVisiting(ModelNode modelNode) throws Exception {
		FileResource pomFileResource = modelNode.getPom();
		if (pomFileResource != null) { //the pom may be missing if the modelNode was created due to a system dependency. We skip creating a ModelNodeInfo for system dependencies.
			String pomChecksum = null;
			if (checksumCalculator != null) {
				pomChecksum = checksumCalculator.getChecksum(pomFileResource.getFile());
			}
			ModelNodeInfo modelNodeInfo = new ModelNodeInfo(modelNode.getGroupId(), modelNode.getArtifactId(), modelNode.getVersion(), pomChecksum, new Long(pomFileResource.getLength()));
			if (modelNode.getParentNode() != null) {
				ModelNodeInfo parentModelNodeInfo = modelNodeInfoResult.getModelNodeInfo(modelNode.getParentNode().getId());
				parentModelNodeInfo.addChild(modelNodeInfo);
			}
			for (ModelNodeArtifact modelNodeArtifact : modelNode.getArtifacts().values()) {
				FileResource artifactFileResource = (FileResource) modelNodeArtifact.getResource();
				String artifactChecksum = null;
				if (checksumCalculator != null) {
					artifactChecksum = checksumCalculator.getChecksum(artifactFileResource.getFile());
				}
				ArtifactInfo artifactInfo = new ArtifactInfo(modelNode.getGroupId(), modelNode.getArtifactId(), modelNode.getVersion(), modelNodeArtifact.getClassifier(), artifactFileResource.getType(), artifactChecksum, artifactFileResource.getLength());
				modelNodeInfo.addArtifact(artifactInfo);
			}
			modelNodeInfoResult.addModelNodeInfo(modelNodeInfo);
		}
	}

	@Override
	public void visit(ModelNodeDependency modelNodeDependency) throws Exception {
		ModelNodeInfo dependantModelNodeInfo = modelNodeInfoResult.getModelNodeInfo(modelNodeDependency.getModelNode().getId());
		DependencyInfo dependencyInfo = new DependencyInfo(modelNodeDependency.getGroupId(), modelNodeDependency.getArtifactId(), modelNodeDependency.getVersion(), modelNodeDependency.getClassifier(), modelNodeDependency.getDependency().getType(), modelNodeDependency.getScope());
		dependantModelNodeInfo.addDependency(dependencyInfo);
	}

}
