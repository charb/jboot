package jboot.repository.client.info.visitor;

import java.util.Set;

import jboot.repository.client.info.ModelNodeInfoResult;
import jboot.repository.client.info.model.ModelNodeInfo;

public class FlatteningNodeInfoVisitor extends AbstractNodeInfoVisitor {

	private Set<ModelNodeInfo> flattenedModelNodeInfos;

	public FlatteningNodeInfoVisitor(ModelNodeInfoResult modelNodeInfoResult, Set<ModelNodeInfo> flattenedModelNodeInfos) {
		super(modelNodeInfoResult);
		this.flattenedModelNodeInfos = flattenedModelNodeInfos;
	}

	public void flatten(String id) throws Exception {
		ModelNodeInfo modelNodeInfo = this.getModelNodeInfoResult().getModelNodeInfo(id);
		if (modelNodeInfo != null) { //make sure the root node of the traversal exists.
			visit(modelNodeInfo);
		}
	}

	@Override
	public void startVisiting(ModelNodeInfo modelNodeInfo) throws Exception {
		flattenedModelNodeInfos.add(modelNodeInfo);
	}

}
