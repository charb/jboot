package jboot.repository.client.info;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jboot.repository.client.info.model.ModelNodeInfo;
import jboot.repository.client.info.model.ModelNodeInfos;
import jboot.repository.client.info.visitor.FlatteningNodeInfoVisitor;

public class ModelNodeInfoResult {
	private Map<String, ModelNodeInfo> modelNodeInfos;

	public ModelNodeInfoResult() {
		this.modelNodeInfos = new HashMap<String, ModelNodeInfo>();
	}

	public boolean hasModelNodeInfo(String id) {
		return this.modelNodeInfos.containsKey(id);
	}

	public ModelNodeInfo getModelNodeInfo(String id) {
		return this.modelNodeInfos.get(id);
	}

	public Map<String, ModelNodeInfo> getModelNodeInfos() {
		return this.modelNodeInfos;
	}

	public void addModelNodeInfo(ModelNodeInfo modelNodeInfo) {
		this.modelNodeInfos.put(modelNodeInfo.getId(), modelNodeInfo);
	}

	public void addModelNodeInfos(Collection<ModelNodeInfo> modelNodeInfos) {
		for (ModelNodeInfo modelNodeInfo : modelNodeInfos) {
			this.modelNodeInfos.put(modelNodeInfo.getId(), modelNodeInfo);
		}
	}

	public ModelNodeInfos getModelNodeInfos(String id) throws Exception {
		Set<ModelNodeInfo> modelNodeInfosSet = new HashSet<ModelNodeInfo>();
		FlatteningNodeInfoVisitor flatteningNodeInfoVisitor = new FlatteningNodeInfoVisitor(this, modelNodeInfosSet);
		flatteningNodeInfoVisitor.flatten(id);
		return new ModelNodeInfos(Arrays.asList(modelNodeInfosSet.toArray(new ModelNodeInfo[0])));
	}

	public void clear() {
		this.modelNodeInfos.clear();
	}
}
