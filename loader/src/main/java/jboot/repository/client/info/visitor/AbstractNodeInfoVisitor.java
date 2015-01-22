package jboot.repository.client.info.visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jboot.repository.client.info.ModelNodeInfoResult;
import jboot.repository.client.info.model.DependencyInfo;
import jboot.repository.client.info.model.ModelInfo;
import jboot.repository.client.info.model.ModelNodeInfo;

public abstract class AbstractNodeInfoVisitor {
	private ModelNodeInfoResult modelNodeInfoResult;
	private Set<String> visited;

	public AbstractNodeInfoVisitor(ModelNodeInfoResult modelNodeInfoResult) {
		this.modelNodeInfoResult = modelNodeInfoResult;
		this.visited = new HashSet<String>();
	}

    public void clearVisited() {
    	visited.clear();
    }

	public void visit(ModelNodeInfo modelNodeInfo) throws Exception {
		visitStarted();
		visitFromParent(modelNodeInfo);
		visitEnded();
	}

	public ModelNodeInfoResult getModelNodeInfoResult() {
		return modelNodeInfoResult;
	}

	private void visitFromParent(ModelNodeInfo modelNodeInfo) throws Exception {
		List<ModelNodeInfo> visitedParents = new ArrayList<ModelNodeInfo>();
		startVisitingParents(modelNodeInfo, visitedParents);
		visitLeaf(modelNodeInfo);
		endVisitingParents(visitedParents);
	}

	private void startVisitingParents(ModelNodeInfo modelNodeInfo, List<ModelNodeInfo> visitedParents) throws Exception {
		ModelInfo parentInfo = modelNodeInfo.getParent();
		if (parentInfo != null) {
			ModelNodeInfo parentNodeInfo = modelNodeInfoResult.getModelNodeInfo(parentInfo.getId());
			if (parentNodeInfo != null && !visited.contains(parentNodeInfo.getId())) {
				visited.add(parentNodeInfo.getId());
				visitedParents.add(parentNodeInfo);
				startVisitingParents(parentNodeInfo, visitedParents);
				startVisiting(parentNodeInfo);
			}
		}
	}

	private void endVisitingParents(List<ModelNodeInfo> visitedParents) throws Exception {
		for (ModelNodeInfo parentNodeInfo : visitedParents) {
			endVisiting(parentNodeInfo);
		}
	}

	private void visitLeaf(ModelNodeInfo modelNodeInfo) throws Exception {
		if (!visited.contains(modelNodeInfo.getId())) {
			visited.add(modelNodeInfo.getId());
			startVisiting(modelNodeInfo);
			for (DependencyInfo dependencyInfo : modelNodeInfo.getDependencies()) {
				visitDependency(dependencyInfo);
			}
			for (ModelInfo childInfo : modelNodeInfo.getChildren()) {
				ModelNodeInfo childNodeInfo = modelNodeInfoResult.getModelNodeInfo(childInfo.getId());
				visitLeaf(childNodeInfo);
			}
			endVisiting(modelNodeInfo);
		}
	}

	private void visitDependency(DependencyInfo dependencyInfo) throws Exception {
		ModelNodeInfo modelNodeInfo = modelNodeInfoResult.getModelNodeInfo(dependencyInfo.getId());
		if (modelNodeInfo != null) {
			visitFromParent(modelNodeInfo);
			visit(dependencyInfo);
		}
	}

	public void visitStarted() throws Exception {
	}

	public void startVisiting(ModelNodeInfo modelNodeInfo) throws Exception {
	}

	public void endVisiting(ModelNodeInfo modelNodeInfo) throws Exception {
	}

	public void visit(DependencyInfo dependencyInfo) throws Exception {
	}

	public void visitEnded() throws Exception {
	}
}
