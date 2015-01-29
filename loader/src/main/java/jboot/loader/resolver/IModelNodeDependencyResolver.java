package jboot.loader.resolver;

import jboot.loader.node.ModelNode;

public interface IModelNodeDependencyResolver {
    public void resolve(ModelNode modelNode) throws Exception;
}
