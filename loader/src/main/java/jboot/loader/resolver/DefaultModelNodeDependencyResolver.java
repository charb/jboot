package jboot.loader.resolver;

import jboot.loader.node.IModelLoader;
import jboot.loader.node.ModelNode;
import jboot.loader.repository.IModelRepositoryLayout;

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
