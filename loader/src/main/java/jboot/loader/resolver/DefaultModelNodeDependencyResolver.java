package jboot.loader.boot.resolver;

import jboot.loader.boot.node.IModelLoader;
import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.repository.IModelRepositoryLayout;

public class DefaultModelNodeDependencyResolver implements IModelNodeDependencyResolver {
	private VisitingModelNodeDependencyResolver visitingModelNodeDependencyResolver;

	public DefaultModelNodeDependencyResolver(ModelNodeResult modelNodeResult, IModelRepositoryLayout repository, IModelLoader modelLoader) {
		visitingModelNodeDependencyResolver = new VisitingModelNodeDependencyResolver(modelNodeResult, repository, modelLoader);
	}

	@Override
	public void resolve(ModelNode modelNode) throws Exception {
		visitingModelNodeDependencyResolver.traverse(modelNode);
	}

}
