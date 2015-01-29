package jboot.loader.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jboot.loader.node.ModelNode;
import jboot.loader.node.ModelNodeDependency;
import jboot.loader.resolver.ModelNodeResult;

public abstract class AbstractNodeVisitor {
    private ModelNodeResult modelNodeResult;
    private Map<String, ModelNode> visited;

    public AbstractNodeVisitor(ModelNodeResult modelNodeResult) {
        this.modelNodeResult = modelNodeResult;
        this.visited = new HashMap<String, ModelNode>();
    }

    public void clearVisited() {
    	visited.clear();
    }

    public void visit(ModelNode modelNode) throws Exception {
        visitStarted();
        visitFromParent(modelNode);
        visitEnded();
    }

    public ModelNodeResult getModelNodeResult() {
		return modelNodeResult;
	}

    private void visitFromParent(ModelNode modelNode) throws Exception {
    	List<ModelNode> visitedParents = new ArrayList<ModelNode>();
        startVisitingParents(modelNode, visitedParents);
        visitLeaf(modelNode);
        endVisitingParents(visitedParents);
    }

    private void startVisitingParents(ModelNode modelNode, List<ModelNode> visitedParents) throws Exception {
        ModelNode parentNode = modelNode.getParentNode();
        if (parentNode != null && !visited.containsKey(parentNode.getId())) {
            visited.put(parentNode.getId(), parentNode);
            visitedParents.add(parentNode);
            startVisitingParents(parentNode, visitedParents);
            startVisiting(parentNode);
        }
    }

    private void endVisitingParents(List<ModelNode> visitedParents) throws Exception {
		for (ModelNode parentNode : visitedParents) {
			endVisiting(parentNode);
		}
	}

    private void visitLeaf(ModelNode modelNode) throws Exception {
        if (!visited.containsKey(modelNode.getId())) {
            visited.put(modelNode.getId(), modelNode);
            startVisiting(modelNode);
            for (ModelNodeDependency modelNodeDependency : modelNode.getResolvedModelNodeDependency()) {
                visitDependency(modelNodeDependency);
            }
            for (ModelNode childNode : modelNode.getChildNodes()) {
                visitLeaf(childNode);
            }
            endVisiting(modelNode);
        }
    }

    private void visitDependency(ModelNodeDependency modelNodeDependency) throws Exception {
        ModelNode modelNode = this.modelNodeResult.getModelNode(modelNodeDependency.getId());
        if (modelNode != null) {
            visitFromParent(modelNode);
            visit(modelNodeDependency);
        }
    }

    public void visitStarted() throws Exception {
    }

    public void startVisiting(ModelNode modelNode) throws Exception {
    }

    public void endVisiting(ModelNode modelNode) throws Exception {
    }

    public void visit(ModelNodeDependency modelNodeDependency) throws Exception {
    }

    public void visitEnded() throws Exception {
    }
}