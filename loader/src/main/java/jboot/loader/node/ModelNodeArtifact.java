package jboot.loader.boot.node;

import jboot.loader.boot.node.resource.Resource;


public class ModelNodeArtifact {
	private ModelNode modelNode; //this is the model node that owns this artifact.
	private String classifier;
	private Resource resource;

	public ModelNodeArtifact(ModelNode modelNode, String classifier, Resource resource) {
		this.modelNode = modelNode;
		this.setClassifier(classifier);
		this.resource = resource;
	}

	public ModelNode getModelNode() {
		return modelNode;
	}

	public void setModelNode(ModelNode modelNode) {
		this.modelNode = modelNode;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		if (classifier == null) {
			classifier = "";
		}
		this.classifier = classifier.trim();
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public String toString(){
		return ""+modelNode+", Classifier:"+classifier;
	}
}
