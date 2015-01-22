package jboot.loader.boot.resolver;

import jboot.loader.boot.node.IModelLoader;
import jboot.loader.boot.repository.IModelRepositoryLayout;

public class DefaultModelNodeResolver implements IModelNodeResolver {
	private VisitingModelNodeBuilder visitingModelNodeBuilder;

	public DefaultModelNodeResolver(ModelNodeResult modelNodeResult, IModelRepositoryLayout repository, IModelLoader modelLoader) {
		visitingModelNodeBuilder = new VisitingModelNodeBuilder(modelNodeResult, repository, modelLoader);
	}

	@Override
	public ExceptionCollector resolve(String groupId, String artifactId, String version) throws Exception {
		return visitingModelNodeBuilder.traverse(groupId, artifactId, version);
	}

}
