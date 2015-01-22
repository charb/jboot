package jboot.loader.boot.resolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jboot.loader.boot.node.ModelNode;

public class ModelNodeResult {
	private Map<String, ModelNode> modelNodes;
	private Map<String, Map<String, ModelNode>> groupedModelNodes; //key = gid:aid , value = (key = ver , value = ModelNode) 
	private Set<String> missingModels; //not found, unable to load, etc.

	public ModelNodeResult() {
		modelNodes = new HashMap<String, ModelNode>();
		groupedModelNodes = new HashMap<String, Map<String,ModelNode>>();
		missingModels = new HashSet<String>();
	}

	public boolean hasModelNode(String id) {
		return modelNodes.containsKey(id);
	}

	public ModelNode getModelNode(String id) {
		return modelNodes.get(id);
	}

	public Map<String, ModelNode> getModelNodes() {
		return modelNodes;
	}

	public ModelNode addModelNode(String groupId, String artifactId, String version, ModelNode node) {
		Map<String, ModelNode> modelNodesByVersion = groupedModelNodes.get(groupId + ":" + artifactId);
		if (modelNodesByVersion == null) {
			modelNodesByVersion = new HashMap<String, ModelNode>();
			groupedModelNodes.put(groupId + ":" + artifactId, modelNodesByVersion);
		}
		modelNodesByVersion.put(version, node);
		return modelNodes.put(ModelNode.getId(groupId, artifactId, version), node);
	}

	public Map<String, ModelNode> getGroupedModelNodes(String groupId, String artifactId) {
		Map<String, ModelNode> modelNodesByVersion = groupedModelNodes.get(groupId + ":" + artifactId);
		if (modelNodesByVersion == null) {
			modelNodesByVersion = new HashMap<String, ModelNode>();
		}
		return modelNodesByVersion;
	}

	public boolean isModelMissing(String id) { //i.e. we tried to load/parse it but couldn't
		return missingModels.contains(id);
	}

	public void addMissingModel(String groupId, String artifactId, String version) {
		missingModels.add(ModelNode.getId(groupId, artifactId, version));
	}

	public void removeMissingModel(String groupId, String artifactId, String version) {
		missingModels.remove(ModelNode.getId(groupId, artifactId, version));
	}

	public Set<String> getMissingModels() {
		return missingModels;
	}

	public void clear() {
		missingModels.clear();
		groupedModelNodes.clear();
		modelNodes.clear();
	}

	public void pruneModels() {
	    for (ModelNode modelNode : modelNodes.values()) {
	        modelNode.prune();
	    }
	}
}
