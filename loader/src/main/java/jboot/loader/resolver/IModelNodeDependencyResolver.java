package jboot.loader.boot.resolver;

import jboot.loader.boot.node.ModelNode;

public interface IModelNodeDependencyResolver {
    public void resolve(ModelNode modelNode) throws Exception;
}
