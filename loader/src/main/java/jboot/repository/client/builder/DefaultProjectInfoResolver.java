package jboot.repository.client.builder;

import java.util.Map;

import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.resolver.ExceptionCollector;
import jboot.loader.boot.resolver.IProjectResolver;
import jboot.loader.boot.resolver.ModelNodeResult;
import jboot.repository.client.info.ModelNodeInfoBuilderVisitor;
import jboot.repository.client.info.ModelNodeInfoResult;

public class DefaultProjectInfoResolver implements IProjectInfoResolver {
	private ModelNodeInfoBuilderVisitor nodeInfoBuilderVisitor;
	private IProjectResolver projectResolver;
	private ModelNodeResult modelNodeResult;

	public DefaultProjectInfoResolver(IProjectResolver projectResolver,	ModelNodeResult modelNodeResult, ModelNodeInfoResult modelNodeInfoResult) {
		this(projectResolver, modelNodeResult, modelNodeInfoResult, true);
	}

	public DefaultProjectInfoResolver(IProjectResolver projectResolver,	ModelNodeResult modelNodeResult, ModelNodeInfoResult modelNodeInfoResult, boolean fillChecksum) {
		this.projectResolver = projectResolver;
		this.modelNodeResult = modelNodeResult;
		this.nodeInfoBuilderVisitor = new ModelNodeInfoBuilderVisitor(modelNodeResult, modelNodeInfoResult, fillChecksum);
	}

	@Override
	public ExceptionCollector resolve(String groupId, String artifactId, String version) throws Exception {
		ExceptionCollector collector = projectResolver.resolve(groupId,	artifactId, version);
		ModelNode modelNode = this.modelNodeResult.getModelNode(ModelNode.getId(groupId, artifactId, version));
		if (modelNode != null) {
			nodeInfoBuilderVisitor.visit(modelNode);
		}
		return collector;
	}

	@Override
	public ExceptionCollector resolve(String groupId, String artifactId) throws Exception {
		ExceptionCollector collector = projectResolver.resolve(groupId,	artifactId);
		Map<String, ModelNode> map = this.modelNodeResult.getGroupedModelNodes(groupId, artifactId);
		for (ModelNode modelNode : map.values()) {
			nodeInfoBuilderVisitor.visit(modelNode);
		}
		return collector;
	}

	@Override
	public void clear() {
		nodeInfoBuilderVisitor.clearVisited();
	}
}
