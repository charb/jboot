package jboot.loader.node;

import jboot.loader.model.Dependency;
import jboot.loader.model.Exclusion;

public class ModelNodeDependency {
    private ModelNode  modelNode; //this the modelnode that depends on this dependency.
    private Dependency dependency;

    public ModelNodeDependency(ModelNode modelNode, Dependency dependency) {
    	this.modelNode = modelNode;
        setDependency(dependency);
    }

    public ModelNodeDependency(ModelNodeDependency modelNodeDependency) {
        this.modelNode = modelNodeDependency.getModelNode();
        this.dependency = modelNodeDependency.getDependency();
    }

	private void evaluateDependencyModelFields() {
		dependency.setGroupId(modelNode.evaluateProperties(dependency.getGroupId()));
		dependency.setArtifactId(modelNode.evaluateProperties(dependency.getArtifactId()));
		dependency.setVersion(modelNode.evaluateProperties(dependency.getVersion()));
		dependency.setClassifier(modelNode.evaluateProperties(dependency.getClassifier()));
		dependency.setScope(modelNode.evaluateProperties(dependency.getScope()));
		dependency.setSystemPath(modelNode.evaluateProperties(dependency.getSystemPath()));
		if (dependency.getExclusions() != null) {
			for (Exclusion exclusion : dependency.getExclusions().getExclusion()) {
				exclusion.setGroupId(modelNode.evaluateProperties(exclusion.getGroupId()));
				exclusion.setArtifactId(modelNode.evaluateProperties(exclusion.getArtifactId()));
			}
		}
	}

	public ModelNode getModelNode() {
        return modelNode;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
        evaluateDependencyModelFields();
    }

	public String getId() {
        return ModelNode.getId(this.getGroupId(), this.getArtifactId(), this.getVersion());
    }

    public String getGroupId() {
        return dependency.getGroupId();
    }

    public String getArtifactId() {
        return dependency.getArtifactId();
    }

    public String getVersion() {
    	return dependency.getVersion();
    }

    public String getClassifier() {
    	if (dependency.getClassifier() == null) {
    		return "";
    	}
    	return dependency.getClassifier();
    }

	public String getScope() {
    	if (dependency.getScope() == null || dependency.getScope().trim().isEmpty()) {
    		return "compile";
    	}
    	return dependency.getScope();
    }

	public String getSystemPath() {
		return dependency.getSystemPath();
	}

    public boolean isOptional() {
   		return dependency.isOptional().booleanValue();
    }

    @Override
    public String toString() {
        return getId();
    }

    /**
     * check that the dependency's groupId and artifactId are valid and non-empty.
     * 
     * @return true if the groupdId and artifacId are valid and non-empty, false otherwise.
     */
    public boolean isValid() {
		String strGroupId = getGroupId();
		String strArtifactId = getArtifactId();
		if (strGroupId != null && !strGroupId.trim().isEmpty() && strArtifactId != null && !strArtifactId.trim().isEmpty()) {
			return true;
		}
		return false;
	}

    public boolean equalsByGidAidClsfr(ModelNodeDependency modelNodeDependency) {
        if (modelNodeDependency.getGroupId() != null && this.getGroupId() != null &&
        	modelNodeDependency.getGroupId().equals(this.getGroupId()) &&
        	modelNodeDependency.getArtifactId() != null && this.getArtifactId() != null &&
        	modelNodeDependency.getArtifactId().equals(this.getArtifactId()) &&
        	modelNodeDependency.getClassifier() != null && this.getClassifier() != null &&
        	modelNodeDependency.getClassifier().equals(this.getClassifier())) {
            return true;
        }
        return false;
    }

	/**
	 * checks if the dependency is required at runtime.
	 * 
	 * @param modelNodeDependency
	 * @return returns false when: [scope = test | provided] [optional = true], otherwise returns true
	 */
	public boolean isRuntimeDependency() {
		boolean bResult = true;
		String strScope = this.getScope();
		if ("test".equals(strScope) /*|| "system".equals(strScope)*/ || "provided".equals(strScope)) {
			bResult = false;
		}
		if (this.isOptional()) {
			bResult = false;
		}
		return bResult;
	}

	public boolean isScopeSystem() {
		return "system".equals(getScope());
	}
	
	public void prune(){
	    dependency=null;
	}
}
