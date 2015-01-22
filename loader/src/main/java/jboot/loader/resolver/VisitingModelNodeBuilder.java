package jboot.loader.boot.resolver;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.boot.model.Model;
import jboot.loader.boot.model.Parent;
import jboot.loader.boot.node.IModelLoader;
import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.node.ModelNodeArtifact;
import jboot.loader.boot.node.ModelNodeDependency;
import jboot.loader.boot.node.resource.FileResource;
import jboot.loader.boot.node.resource.Resource;
import jboot.loader.boot.repository.IModelRepositoryLayout;

public class VisitingModelNodeBuilder extends QueueVisitor<ModelNode> {
	private static final Logger log = Logger.getLogger(VisitingModelNodeBuilder.class.getName());
	private static final Logger resolverLog = Logger.getLogger("jboot.resolver");

	private ModelNodeResult modelNodeResult;
	private IModelRepositoryLayout repository;
	private IModelLoader modelLoader;
	private ExceptionCollector exceptionCollector;

	public VisitingModelNodeBuilder() {
		super();
	}

	public VisitingModelNodeBuilder(ModelNodeResult modelNodeResult, IModelRepositoryLayout repository, IModelLoader modelLoader) {
		this();
		this.modelNodeResult = modelNodeResult;
		this.repository = repository;
		this.modelLoader = modelLoader;
		this.exceptionCollector = new ExceptionCollector();
	}

	//entry point method
	public ExceptionCollector traverse(String strGroupId, String strArtifactId, String strVersion) {
		ModelNode initialModelNode = getModelNode(strGroupId, strArtifactId, strVersion);
		if (initialModelNode != null) {
			enqueue(initialModelNode);
			traverse();
		}
		return exceptionCollector;
	}

	@Override
	protected void visit(ModelNode item) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "visiting: " + item.getId());
		}

		buildAncestorsList(item); //this method also links parent nodes to their child. (we ignore the return value because we don't need it.

		item.buildPropertiesFromAncestors();

		item.buildDependenciesFromAncestors();

		for (ModelNodeDependency modelNodeDependency : item.getModelNodeDependencies().values()) {
			String strVersion = modelNodeDependency.getVersion();
			if (strVersion == null || strVersion.trim().isEmpty()) {
				if (resolverLog.isLoggable(Level.WARNING)) {
					resolverLog.log(Level.WARNING, "Undetermined dependency version of " + modelNodeDependency.getGroupId() + ":" + modelNodeDependency.getArtifactId() + " in model " + item.getId());
				}
				continue; //skip to next dependency.
			}
			//skip unneeded depdendencies [scope = test | provided] [optional = true]
			if (!modelNodeDependency.isRuntimeDependency()) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Skipping non-runtime dependency " + modelNodeDependency.getId() + " in model " + item.getId());
				}
				continue; //skip to next dependency.
			}

			if (modelNodeDependency.isScopeSystem()) {
				ModelNode sysModelNode = buildSystemModelNode(modelNodeDependency);
				if (sysModelNode != null) {
					sysModelNode.addDependant(item);
				} else {
					if (resolverLog.isLoggable(Level.WARNING)) {
						resolverLog.log(Level.WARNING, "Could not find artifact" + ((modelNodeDependency.getSystemPath() != null)?" at " + modelNodeDependency.getSystemPath():"") + " for system scoped dependency " + modelNodeDependency.getId() + " referenced from " + item.getId() + ". The system scoped dependency will be ignored.");
					}
				}
			} else {
				//attempt to load the dependency's model (the method getModelNode will also attempt to enqueue the loaded ModelNode)
				ModelNode dependencyModelNode = getModelNode(modelNodeDependency.getGroupId(), modelNodeDependency.getArtifactId(), modelNodeDependency.getVersion());
	
				if (dependencyModelNode != null) {
					//add the current ModelNode as a dependant in the ModelNode of the dependency
					dependencyModelNode.addDependant(item);
				} else {
					if (resolverLog.isLoggable(Level.WARNING)) {
						resolverLog.log(Level.WARNING, "Could not load POM of dependency " + modelNodeDependency.getId() + " referenced from " + item.getId());
					}
				}
			}
		}

		resolveNodeArtifacts(item);
	}

	private void resolveNodeArtifacts(ModelNode modelNode) {
		FileResource pom = repository.getPomResource(modelNode.getGroupId(), modelNode.getArtifactId(), modelNode.getVersion());
		modelNode.setPom(pom);
		repository.fillAllArtifacts(modelNode);
	}

	private List<ModelNode> buildAncestorsList(ModelNode modelNode) {
		List<ModelNode> ancestors = new LinkedList<ModelNode>();
		Parent parent = modelNode.getParent();
		ModelNode childModelNode = modelNode;
		while (parent != null) {
			ModelNode parentModelNode = getModelNode(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
			if (parentModelNode == null) {
				break;
			}
			if (!parentModelNode.getChildNodes().contains(childModelNode)) {
				parentModelNode.addChild(childModelNode);
			}
			childModelNode = parentModelNode;
			ancestors.add(0, parentModelNode);
			parent = parentModelNode.getParent();
		}
		return ancestors;
	}

	private ModelNode getModelNode(String groupId, String artifactId, String version) {
		return getModelNode(groupId, artifactId, version, true);
	}

	private ModelNode getModelNode(String groupId, String artifactId, String version, boolean enqueueAllVersions) {
		String modelId = ModelNode.getId(groupId, artifactId, version);
		ModelNode modelNode = modelNodeResult.getModelNode(modelId);
		if (modelNode == null && !modelNodeResult.isModelMissing(modelId)) {
			Model model = loadModel(groupId, artifactId, version);
			if (model != null) {
				modelNode = new ModelNode(model);
				modelNodeResult.addModelNode(groupId, artifactId, version, modelNode);
				enqueue(modelNode);
			} else {
				modelNodeResult.addMissingModel(groupId, artifactId, version);
			}
			if (enqueueAllVersions == true) {
				enqueueAllVersions(groupId, artifactId);
			}
		}
		return modelNode;
	}

	private ModelNode buildSystemModelNode(ModelNodeDependency modelNodeDependency) {
		ModelNode sysModelNode = modelNodeResult.getModelNode(modelNodeDependency.getId());
		if (sysModelNode == null && !modelNodeResult.isModelMissing(modelNodeDependency.getId())) {
			String systemPath = modelNodeDependency.getSystemPath();
			File sysArtifactFile = null;
			if (systemPath != null && (sysArtifactFile = new File(systemPath)).exists()) {
				//create a model for the system dependency
				Model sysModel = new Model();
				sysModel.setGroupId(modelNodeDependency.getGroupId());
				sysModel.setArtifactId(modelNodeDependency.getArtifactId());
				sysModel.setVersion(modelNodeDependency.getVersion());
				int lastDot = systemPath.lastIndexOf('.');
				if (lastDot < 0) {
					sysModel.setPackaging("jar");
				} else {
					sysModel.setPackaging(systemPath.substring(lastDot + 1));
				}

				//create modelNode for system dependency
				sysModelNode = new ModelNode(sysModel);

				//create artifact for system dependency
				Resource res = Resource.createResource(sysArtifactFile); 
				ModelNodeArtifact sysArtifact = new ModelNodeArtifact(sysModelNode, null, res);
				sysModelNode.addArtifact(sysArtifact);

				modelNodeResult.addModelNode(sysModel.getGroupId(), sysModel.getArtifactId(), sysModel.getVersion(), sysModelNode);
			} else {
				modelNodeResult.addMissingModel(modelNodeDependency.getGroupId(), modelNodeDependency.getArtifactId(), modelNodeDependency.getVersion());
			}
		}
		return sysModelNode;
	}

	private Model loadModel(String groupId, String artifactId, String version) {
		FileResource pomResource = repository.getPomResource(groupId, artifactId, version);
		Model model = null;
		if (pomResource != null) {
			try {
				model = modelLoader.load(pomResource.getFile());

 				inheritModelParentFields(model);
				fillModelDefaults(model);

			} catch (Exception ex) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Error parsing POM " + groupId + ":" + artifactId + ":" + version, ex);
				}
				exceptionCollector.add(new Exception("Error parsing POM " + groupId + ":" + artifactId + ":" + version, ex));
			}
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "POM not found " + groupId + ":" + artifactId + ":" + version);
			}
		}
		return model;
	}

	private void fillModelDefaults(Model model) {
        if (model.getPackaging() == null || model.getPackaging().trim().equals("")) {
        	model.setPackaging("jar");
        }
	}

	/**
	 * Sets the groupId and the version of this model from the parent element.</br>
	 * The values are set <em>only</em> when the model does not specify them explicitly.</br>
	 * If a parent element does not exist in the model, then nothing is modified.</br>
	 * 
	 * Additionally, this method trims the groupId, artifactId, and version of the model and its parent section.
	 * 
	 * @param model The model for which to set the groupId and version.
	 * 
	 */
	private void inheritModelParentFields(Model model) {
		//trim fields
		if (model.getGroupId() != null) {
			model.setGroupId(model.getGroupId().trim());
		}
		if (model.getArtifactId() != null) {
			model.setArtifactId(model.getArtifactId().trim());
		}
		if (model.getVersion() != null) {
			model.setVersion(model.getVersion().trim());
		}

		Parent parent = model.getParent();
		if (parent != null) {
			//trim parent fields
			if (parent.getGroupId() != null) {
				parent.setGroupId(parent.getGroupId().trim());
			}
			if (parent.getArtifactId() != null) {
				parent.setArtifactId(parent.getArtifactId().trim());
			}
			if (parent.getVersion() != null) {
				parent.setVersion(parent.getVersion().trim());
			}

			//inherit
			if (model.getGroupId() == null || model.getGroupId().isEmpty()) {
				model.setGroupId(parent.getGroupId());
			}
			if (model.getVersion() == null || model.getVersion().isEmpty()) {
				model.setVersion(parent.getVersion());
			}
		}
	}

	private void enqueueAllVersions(String groupId, String artifactId) {
		String versions[] = repository.getVersions(groupId, artifactId);
		if (versions != null) {
			for (String strVersion: versions) {
				getModelNode(groupId, artifactId, strVersion, false);
			}
		}
	}
}
