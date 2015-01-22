package jboot.loader.boot.resolver;

import jboot.loader.boot.node.IModelLoader;
import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.repository.IModelRepositoryLayout;

public class DefaultProjectResolver implements IProjectResolver {
    private ModelNodeResult modelNodeResult;
	private IModelRepositoryLayout repository;
    private DefaultModelNodeResolver defaultModelNodeResolver;
    private DefaultModelNodeDependencyResolver defaultModelNodeDependencyResolver;

    public DefaultProjectResolver(ModelNodeResult modelNodeResult, IModelRepositoryLayout repository, IModelLoader modelLoader) {
    	this.modelNodeResult = modelNodeResult;
		this.repository = repository;
    	defaultModelNodeResolver = new DefaultModelNodeResolver(modelNodeResult, repository, modelLoader);
    	defaultModelNodeDependencyResolver = new DefaultModelNodeDependencyResolver(modelNodeResult, repository, modelLoader);
	}

	@Override
	public ExceptionCollector resolve(String groupId, String artifactId, String version) throws Exception {
		ExceptionCollector exceptionCollector = defaultModelNodeResolver.resolve(groupId, artifactId, version);
		ModelNode modelNode = modelNodeResult.getModelNode(ModelNode.getId(groupId, artifactId, version));
		if (modelNode != null) {
			defaultModelNodeDependencyResolver.resolve(modelNode);
		}
		return exceptionCollector;
	}

	@Override
	public ExceptionCollector resolve(String groupId, String artifactId) throws Exception {
		if (groupId == null || groupId.trim().isEmpty()) {
			throw new IllegalArgumentException("groupId is empty.");
		}
		if (artifactId == null || artifactId.trim().isEmpty()) {
			throw new IllegalArgumentException("artifactId is empty.");
		}
		ExceptionCollector exceptionCollector = new ExceptionCollector();
        String[] versions = repository.getVersions(groupId, artifactId);
        if (versions != null) {
            for (String version : versions) {
        		ExceptionCollector modelNodeResolverExceptions = defaultModelNodeResolver.resolve(groupId, artifactId, version);
        		exceptionCollector.merge(modelNodeResolverExceptions);
            }

            for (String version : versions) {
        		ModelNode modelNode = modelNodeResult.getModelNode(ModelNode.getId(groupId, artifactId, version));
        		if (modelNode != null) {
	        		defaultModelNodeDependencyResolver.resolve(modelNode);
        		}
            }
        } else {
        	throw new Exception("No versions found for: " + groupId + ":" + artifactId);
        }
		return exceptionCollector;
	}

}
