package jboot.loader.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jboot.loader.model.Dependency;
import jboot.loader.model.Dependency.Exclusions;
import jboot.loader.model.Exclusion;
import jboot.loader.model.Model;
import jboot.loader.model.Parent;
import jboot.loader.node.resource.FileResource;

public class ModelNode {
	private static final Logger resolverLog = Logger.getLogger("jboot.resolver");
	private Model model;
    private String groupId;
	private String artifactId;
	private String version;
	private String packaging;
	private ModelNode parentNode;
	private List<ModelNode> childNodes;
	private Map<String, ModelNodeDependency> modelNodeDependencies; //these are the deps of the model. including ones not having a ModelNode and ones not having any artifacts. 
	private List<ModelNodeDependency> resolvedNodeDependencies; //only dependencies of this node whose artifacts exist. (and effectively whose ModelNode exists)
	private List<ModelNode> dependants;
	private Map<String, ModelNodeArtifact> artifacts; //this is filled by the noderesolver not the deps resolver
	private Map<String, String> propertiesMap; //Holds agregated properties of this node's model and all its ancestors.
	private FileResource pom;
	private long modelNodeId;
	private static long uniqueID;

	public ModelNode() {
		this.childNodes = new ArrayList<ModelNode>();
		this.modelNodeDependencies = new LinkedHashMap<String, ModelNodeDependency>();
		this.resolvedNodeDependencies = new ArrayList<ModelNodeDependency>();
		this.dependants = new ArrayList<ModelNode>();
		this.artifacts = new HashMap<String, ModelNodeArtifact>();
		synchronized (ModelNode.class) {
			modelNodeId = uniqueID++;
		}
	}

	public ModelNode(Model model) {
		this();
		this.model = model;
		this.groupId = model.getGroupId();
		this.artifactId = model.getArtifactId();
		this.version = model.getVersion();
		this.packaging = model.getPackaging();
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getPackaging() {
	    return packaging;
	}

	private Model getModel() { //private!
        if (model == null) {
            throw new IllegalStateException("The model has been pruned out of model-node: " + this.getId());
        }
	    return model;
	}

	public Parent getParent() {
	    return getModel().getParent();
	}

	public ModelNode getParentNode() {
		return parentNode;
	}

	public void addChild(ModelNode modelNode) {
		childNodes.add(modelNode);
		modelNode.parentNode = this;
	}

	public List<ModelNode> getChildNodes() {
		return childNodes;
	}

	public List<ModelNodeDependency> getResolvedModelNodeDependency() {
		return resolvedNodeDependencies;
	}

	public void addResolvedNodeDependency(ModelNodeDependency nodeDependency) {
		if (!resolvedNodeDependencies.contains(nodeDependency))
			resolvedNodeDependencies.add(nodeDependency);
	}

	public boolean hasModelNodeDependency(String id) {
		return modelNodeDependencies.containsKey(id);
	}

	public void addModelNodeDependency(ModelNodeDependency modelNodeDependency) {
		modelNodeDependencies.put(modelNodeDependency.getId(), modelNodeDependency);
	}

	public Map<String, ModelNodeDependency> getModelNodeDependencies() {
		return modelNodeDependencies;
	}

	public List<ModelNode> getDependants() {
		return dependants;
	}

	public void addDependant(ModelNode dependantModelNode) {
		this.dependants.add(dependantModelNode);
	}

	public String getId() {
		return getId(this.getGroupId(), this.getArtifactId(), this.getVersion());
	}

	public static String getId(String groupId, String artifactId, String version) {
		return groupId + ":" + artifactId + ":" + version;
	}

	public String toString() {
		return getId();
	}

	public Map<String, ModelNodeArtifact> getArtifacts() {
		return Collections.unmodifiableMap(artifacts);
	}

	public ModelNodeArtifact getArtifact(String classifier) {
		if (classifier == null) {
			classifier = "";
		}
		return artifacts.get(classifier.trim());
	}

	public void addArtifact(ModelNodeArtifact modelNodeArtifact) {
		artifacts.put(modelNodeArtifact.getClassifier(), modelNodeArtifact);
	}

	public Map<String, String> getPropertiesMap() {
		return propertiesMap;
	}

	public long getModelNodeId() {
		return modelNodeId;
	}

	public void setModelNodeId(long modelNodeId) {
		this.modelNodeId = modelNodeId;
	}

	public FileResource getPom() {
		return pom;
	}

	public void setPom(FileResource pom) {
		this.pom = pom;
	}

	public void buildPropertiesFromAncestors() {
		if (parentNode != null) {
			parentNode.buildPropertiesFromAncestors();
		}
		if (propertiesMap == null) {
			propertiesMap = new HashMap<String, String>();
			if (parentNode != null) {
				//add the parent's properties to this node's properties.
				propertiesMap.putAll(parentNode.getPropertiesMap());
			}
			addDefaultProperties(propertiesMap); //this will override the parent's default properties.
	        Map<String, String> rawProperties = getModel().getProperties() != null ? getModel().getProperties().getProperties() : null;
			if (rawProperties != null) {
				propertiesMap.putAll(rawProperties);
			}

			//evaluate the property values of the model from the aggregated properties
			if (parentNode != null) {
				for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
					String strEvaluatedValue = ModelNode.evaluateProperties(propertiesMap, entry.getValue());
					entry.setValue(strEvaluatedValue);
				}
			}

			//evaluate the model fields that are returned from this class
			evaluateModelFields();
		}
	}

	private void addDefaultProperties(Map<String, String> propertiesMap) {
		propertiesMap.put("project.groupId", getGroupId());
		propertiesMap.put("project.artifactId", getArtifactId());
		propertiesMap.put("project.packaging", getPackaging());
		propertiesMap.put("project.version", getVersion());
		propertiesMap.put("pom.groupId", getGroupId());
		propertiesMap.put("pom.artifactId", getArtifactId());
		propertiesMap.put("pom.packaging", getPackaging());
		propertiesMap.put("pom.version", getVersion());
		propertiesMap.put("artifactId", getArtifactId());
	}

	private void evaluateModelFields() {
		groupId = evaluateProperties(groupId);
		artifactId = evaluateProperties(artifactId);
		version = evaluateProperties(version);
		//TODO: do we need to do evaluation for packaging and parent?
	}

    private static Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");

	public static String evaluateProperties(Map<String, String> propertiesMap, String strExpression) {
		if (strExpression == null) {
			return null;
		}
        if (strExpression.indexOf('$') == -1){
            return strExpression;
        }
		StringBuffer strBuffer = new StringBuffer();
        boolean hasAReplacement=false;
		Matcher matcher = pattern.matcher(strExpression);
		while (matcher.find()) {
			String strPropertyKey = matcher.group(1);
			String strPropertyValue = propertiesMap.get(strPropertyKey);
			//TODO: dynamic property resolution through the model fields can be invoked here.
			if (strPropertyValue == null) {
				//try system properties.
				strPropertyValue = System.getProperty(strPropertyKey);
			}
			if (strPropertyValue != null) {
                hasAReplacement=true;
				strPropertyValue = strPropertyValue.replaceAll("\\\\", "\\\\\\\\"); //escape '\'
				strPropertyValue = strPropertyValue.replaceAll("\\$", "\\\\\\$"); //escape '$'
				matcher.appendReplacement(strBuffer, strPropertyValue);
			}
		}
        if (hasAReplacement){
            matcher.appendTail(strBuffer);
            return strBuffer.toString();
        }else{
            return strExpression;
        }
	}

	public String evaluateProperties(String strExpression) {
		return evaluateProperties(this.propertiesMap, strExpression);
	}

	public void buildDependenciesFromAncestors() {
		Map<String, Dependency> mgmtDependencies = new HashMap<String, Dependency>(); //key = gid:aid //value = Dependency model object
		Map<String, Dependency> dependencies = new LinkedHashMap<String, Dependency>(); //key = gid:aid //value = Dependency model object
		buildDependenciesFromAncestors(mgmtDependencies, dependencies);
//		modelNodeDependencies.clear(); //removed because subsequent calls will have rawDependencies and rawDependencyManagement set to null see buildDependenciesFromAncestors()
		for (Dependency dependency : dependencies.values()) {
			ModelNodeDependency modelNodeDependency = new ModelNodeDependency(this, new Dependency(dependency));
			inheritFromDependencyMgmt(mgmtDependencies, modelNodeDependency.getDependency());
			//TODO: implement treatment for version ranges here?
			addModelNodeDependency(modelNodeDependency);
		}
	}

	/**
	 * Aggregates the dependencies in the dependencyManagement and dependencies sections of a model, in aggregation maps mgmtDependencies and dependencies respectively.</br>
	 * No check is made to see if a dependency already exists in the map. In which case the dependency is overwritten.</br>
	 * NOTE: if a duplicate dependency definition is found, the one closest to this model in its ancestors chain will be used.
	 * 
	 * @param mgmtDependencies Valid dependencies from the dependencyManagement section are added to this map. This map's key is formatted as follows groupId:artifactId
	 * @param dependencies Valid dependencies from the dependencies section are added to this map. This map's key is formatted as follows groupId:artifactId
	 */
	private void buildDependenciesFromAncestors(Map<String, Dependency> mgmtDependencies, Map<String, Dependency> dependencies) {
		if (parentNode != null) {
			parentNode.buildDependenciesFromAncestors(mgmtDependencies, dependencies);
		}
        List<Dependency> rawDependencyManagement = getModel().getDependencyManagement() != null ? getModel().getDependencyManagement().getDependencies() != null ? getModel().getDependencyManagement().getDependencies().getDependency() : null : null;
		if (rawDependencyManagement != null) {
			aggregateDependencies(mgmtDependencies, rawDependencyManagement, null);
		}
        List<Dependency> rawDependencies = getModel().getDependencies() != null ? getModel().getDependencies().getDependency() : null;
		if (rawDependencies != null) {
			aggregateDependencies(dependencies, rawDependencies, null);
		}
	}

	/**
	 * Adds all the dependencies into the aggregation map.</br>
	 * No check is made to see if a dependency already exists in the map. In which case the dependency is overwritten.</br>
	 * If a dependency does not have a groupId and/or an artifactId, then it is added to the skippedDependencies list.</br>
	 *
	 * @param aggregationMap Valid dependencies are added to this map. This map's key is formatted as follows groupId:artifactId
	 * @param dependencies List of input Dependency objects
	 * @param skippedDependencies List of invalid dependencies. This parameter can be null.
	 */
	private void aggregateDependencies(Map<String, Dependency> aggregationMap, List<Dependency> dependencies, List<Dependency> skippedDependencies) {
		for (Dependency dependency: dependencies) {
			if (dependency.isValid()) {
				String strGid = evaluateProperties(dependency.getGroupId());
				String strAid = evaluateProperties(dependency.getArtifactId());
				aggregationMap.put(strGid + ":" + strAid, dependency);
			} else {
				if (resolverLog.isLoggable(Level.WARNING)) {
					resolverLog.log(Level.WARNING, "Ignoring invalid dependency " + dependency.getId() + " referenced from " + this.getId() + " or one of its ancestors.");
				}
				if (skippedDependencies != null) {
					skippedDependencies.add(dependency);
				}
			}
		}
	}

	/**
	 * Tries to inherit missing fields in a dependency from the aggregated dependency management data.
	 * 
	 * @param mgmtDependencies the aggregated dependencies from the dependecyManagement sections.
	 * @param dependency the dependency for which to fill the inherited fields.
	 */
	private void inheritFromDependencyMgmt(Map<String, Dependency> mgmtDependencies, Dependency dependency) {
		Dependency mgmtDependency = mgmtDependencies.get(dependency.getGroupId() + ":" + dependency.getArtifactId());
		if (mgmtDependency != null) { 
			if (dependency.getVersion() == null || dependency.getVersion().trim().isEmpty()) {
				dependency.setVersion(evaluateProperties(mgmtDependency.getVersion()));

				//inheritance of other fields like scope and classifier is only done if the version has been inherited.
				if (dependency.getScope() == null || dependency.getScope().trim().isEmpty()) {
					dependency.setScope(evaluateProperties(mgmtDependency.getScope()));
				}

				if (dependency.getClassifier() == null || dependency.getClassifier().trim().isEmpty()) {
					dependency.setClassifier(evaluateProperties(mgmtDependency.getClassifier()));
				}

				if (dependency.getSystemPath() == null || dependency.getSystemPath().trim().isEmpty()) {
					dependency.setSystemPath(evaluateProperties(mgmtDependency.getSystemPath()));
				}

				//inherit exclusions.
				if (mgmtDependency.getExclusions() != null) {
					if (dependency.getExclusions() == null) {
						dependency.setExclusions(new Exclusions());
					}
					for (Exclusion mgmtExclusion : mgmtDependency.getExclusions().getExclusion()) {
						boolean bFound = false;
						mgmtExclusion.setGroupId(evaluateProperties(mgmtExclusion.getGroupId()));
						mgmtExclusion.setArtifactId(evaluateProperties(mgmtExclusion.getArtifactId()));
						for (Exclusion depExclusion : dependency.getExclusions().getExclusion()) {
							if (depExclusion.getGroupId().equals(mgmtExclusion.getGroupId()) && depExclusion.getArtifactId().equals(mgmtExclusion.getArtifactId())) {
								bFound = true;
								break;
							}
						}
						if (bFound == false) {
							dependency.getExclusions().getExclusion().add(mgmtExclusion);
						}
					}
				}

				//the optional field is not inherited. (it can never be null when loaded from xml)
//				if (dependency.isOptional() == null) {
//					modelNodeDependency.setInheritedOptional(mgmtDependency.isOptional());
//				}
			}
		}
	}

	/**
	 * Checks if a dependency's groupId, artifactId are excluded by all the dependents of this ModelNode
	 * 
	 * @param groupId
	 * @param artifactId
	 * @return true if the specified groupId:artifactId is excluded. false otherwise
	 */
	public boolean isDependencyExcluded(String groupId, String artifactId) {
		//check if all the dependents exclude groupId:artifactId when they reference this node as a dependency.
		String thisNodeId = getId();
		for (ModelNode dependentNode : dependants) {
			ModelNodeDependency thisNodeDependency = dependentNode.getModelNodeDependencies().get(thisNodeId);
			if (thisNodeDependency.getDependency().getExclusions() != null) {
				List<Exclusion> exclusions = thisNodeDependency.getDependency().getExclusions().getExclusion();
				boolean hasExclusion = false;
				for (Exclusion exclusion : exclusions) {
					if (exclusion.getGroupId().equals(groupId) && exclusion.getArtifactId().equals(artifactId)) {
						hasExclusion = true;
						break;
					}
				}
				if (hasExclusion == false) {
					return false; // the dependentNode does not exclude groupId:artifactId when referring to this ModelNode.
				}
			} else {
				return false; // the dependentNode does not have an exclusions section when referring to this ModelNode.
			}
		}
		return true; // all the dependants exclude groupId:artifactId when the refer to this modelNode as a dependency.
	}

	public void prune() {
	    dependants=null;
	    modelNodeDependencies=null;
	    resolvedNodeDependencies=null;
	    propertiesMap=null;
	    this.model = null;
//	    if (modelNodeDependencies!=null){
//	        for (ModelNodeDependency mdp : modelNodeDependencies.values()){
//	            mdp.prune();
//	        }
//	    }
//	    if (resolvedNodeDependencies!=null){
//	        for (ModelNodeDependency mdp : resolvedNodeDependencies){
//                mdp.prune();
//            }
//	    }	    
	    //TODO: maybe we can also prune modelNodeDependencies, resolvedNodeDependencies 
	}
}
