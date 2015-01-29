package jboot.loader.resolver;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.model.Dependency;
import jboot.loader.node.IModelLoader;
import jboot.loader.node.ModelNode;
import jboot.loader.node.ModelNodeArtifact;
import jboot.loader.node.ModelNodeDependency;
import jboot.loader.repository.IModelRepositoryLayout;

public class VisitingModelNodeDependencyResolver extends QueueVisitor<ModelNode> {
	private static final Logger log = Logger.getLogger(VisitingModelNodeDependencyResolver.class.getName());
	private static final Logger resolverLog = Logger.getLogger("jboot.resolver");

	private ModelNodeResult modelNodeResult;
	private IModelRepositoryLayout repository;

	public VisitingModelNodeDependencyResolver(ModelNodeResult modelNodeResult, IModelRepositoryLayout repository, IModelLoader modelLoader) {
		this.modelNodeResult = modelNodeResult;
		this.repository = repository;
	}

	public void traverse(ModelNode initialModelNode) {
		if (initialModelNode != null) {
			enqueue(initialModelNode);
			traverse();
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Initial model is null. Dependency resolution aborted." );
			}
		}
	}

	@Override
	protected void visit(ModelNode item) {
		//enqueue the parent
		ModelNode parentModelNode = item.getParentNode();
		if (parentModelNode != null) {
			enqueue(parentModelNode);
		}

		//for each dependency resolve it by checking it's artifact
		//if it is resolved enqueue it's modelnode.
		Map<String, ModelNodeDependency> dependencies = item.getModelNodeDependencies();
		for (ModelNodeDependency modelNodeDependency: dependencies.values()) {
			if (modelNodeDependency.isValid()) {
				ModelNodeDependency resolvedModelNodeDependency = null;

				//filter unneeded depdendencies [scope = test | provided] [optional = true]
				if (!modelNodeDependency.isRuntimeDependency()) {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Skipping non-runtime dependency " + modelNodeDependency.getId() + " in model " + item.getId());
					}
					continue;
				}

				//check artifact
				if (findArtifact(modelNodeDependency) != null) {
					resolvedModelNodeDependency = new ModelNodeDependency(modelNodeDependency);
				} else {
					//check if excluded before trying to resolve to another version
					if (item.isDependencyExcluded(modelNodeDependency.getGroupId(), modelNodeDependency.getArtifactId())) {
						if (resolverLog.isLoggable(Level.FINER)) {
							resolverLog.log(Level.FINER, "Excluding dependency " + modelNodeDependency.getId() + " referenced from " + item.getId());
						}
						continue;
					}

					if (resolverLog.isLoggable(Level.FINER)) {
						resolverLog.log(Level.FINER, "Searching for alternative of " + modelNodeDependency.getId() + " in transitive dependencies...");
					}
					//look in the transitive dependencies
					TransitiveDependenciesVisitor transitiveDependenciesVisitor = new TransitiveDependenciesVisitor(this.modelNodeResult);
					resolvedModelNodeDependency = transitiveDependenciesVisitor.findAlternativeDependency(modelNodeDependency);

					//look in the transitive dependants
					if (resolvedModelNodeDependency == null) {
						if (resolverLog.isLoggable(Level.FINER)) {
							resolverLog.log(Level.FINER, "Searching for alternative of " + modelNodeDependency.getId() + " in transitive dependants...");
						}
						TransitiveDependantsVisitor transitiveDependantsVisitor = new TransitiveDependantsVisitor();
						resolvedModelNodeDependency = transitiveDependantsVisitor.findAlternativeDependency(modelNodeDependency);
					}

					//look in repository for another version
					if (resolvedModelNodeDependency == null) {
						if (resolverLog.isLoggable(Level.FINER)) {
							resolverLog.log(Level.FINER, "Searching for alternative of " + modelNodeDependency.getId() + " in local repository...");
						}
						resolvedModelNodeDependency = getNewDependencyFromRepository(modelNodeDependency);							
					}

					if (resolvedModelNodeDependency != null){
						if (resolverLog.isLoggable(Level.INFO)) {
							resolverLog.log(Level.INFO, "Found " + resolvedModelNodeDependency.getId() + " as alternative of " + modelNodeDependency.getId() + ".");
						}
					}else{
						if (resolverLog.isLoggable(Level.WARNING)) {
							resolverLog.log(Level.WARNING, "Could not find artifact of dependency: " + modelNodeDependency.getId() + " referenced from " + item.getId());
						}
					}
				}

				if (resolvedModelNodeDependency != null) {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Resolved " + resolvedModelNodeDependency.getId() + " as dependency of model " + resolvedModelNodeDependency.getModelNode().getId());
					}
					item.addResolvedNodeDependency(resolvedModelNodeDependency);

					//get the modelNode of the dependency in order to enqueue it.
					ModelNode dependencyModelNode = modelNodeResult.getModelNode(resolvedModelNodeDependency.getId()); //this is the modelNode of the dependency.
                    enqueue(dependencyModelNode);
				} else {
					if (resolverLog.isLoggable(Level.WARNING)) {
						resolverLog.log(Level.WARNING, "Ignoring dependency " + modelNodeDependency.getId() + " referenced from " + item.getId() + ", no artifact or alternative artifact could be found.");
					}
				}

			} else {
				if (resolverLog.isLoggable(Level.WARNING)) {
					resolverLog.log(Level.WARNING, "Bad dependency specification " + modelNodeDependency.getId() + " for model " + modelNodeDependency.getModelNode().getId());
				}
			}
		}
	}

	private ModelNodeArtifact findArtifact(ModelNodeDependency modelNodeDependency) {
		ModelNodeArtifact modelNodeArtifact = null;
		ModelNode modelNode = modelNodeResult.getModelNode(modelNodeDependency.getId());
		if (modelNode != null) {
			modelNodeArtifact = modelNode.getArtifact(modelNodeDependency.getClassifier());
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.warning("Dependency " + modelNodeDependency.getId() + " does not have a corresponding model.");
			}
		}
		return modelNodeArtifact;
	}

	/**
	 * Searches all versions of the artifacts in the repository having the same groupId and artifactId as the unresolved dependency.
	 * The search is done from the highest version to the lowest.
	 * 
	 * @param unresolvedModelNodeDependency the dependency whose artifact could not be resolved.
	 * @return a new ModelNodeDependency whose artifact is resolved.
	 */
	private ModelNodeDependency getNewDependencyFromRepository(ModelNodeDependency unresolvedModelNodeDependency) {
		String versions[] = repository.getVersions(unresolvedModelNodeDependency.getGroupId(), unresolvedModelNodeDependency.getArtifactId());
		if (versions != null) {
			for (int i=versions.length-1;i>=0;i--) { //from highest to lowest version.
				String strVersion = versions[i];
				Dependency newDependency = new Dependency(unresolvedModelNodeDependency.getDependency()); //copy the old dependency model object
				newDependency.setVersion(strVersion);
				ModelNodeDependency newModelNodeDependency = new ModelNodeDependency(unresolvedModelNodeDependency.getModelNode(), newDependency);
				ModelNodeArtifact modelNodeArtifact = findArtifact(newModelNodeDependency);
				if (modelNodeArtifact != null) {
					return newModelNodeDependency;
				}
			}
		}
		return null;
	}

	private class TransitiveDependenciesVisitor extends QueueVisitor<ModelNode> {
		private ModelNodeResult modelNodeResult;
		private ModelNodeDependency unresolvedModelNodeDependency;
		private ModelNodeDependency resolvedModelNodeDependency;

		public TransitiveDependenciesVisitor(ModelNodeResult modelNodeResult) {
			this.modelNodeResult = modelNodeResult;
		}

		public ModelNodeDependency findAlternativeDependency(ModelNodeDependency unresolvedModelNodeDependency) {
			ModelNodeDependency newModelNodeDependency = null;
			this.unresolvedModelNodeDependency = unresolvedModelNodeDependency;
			this.resolvedModelNodeDependency = null;
			enqueue(this.unresolvedModelNodeDependency.getModelNode()); //enqueue the dependant of the unresolved dependency.
			this.traverse();
			if (resolvedModelNodeDependency != null) {
				Dependency newDependency = new Dependency(resolvedModelNodeDependency.getDependency());
				newModelNodeDependency = new ModelNodeDependency(unresolvedModelNodeDependency.getModelNode(), newDependency);
			}
			return newModelNodeDependency;
		}

		@Override
		protected void visit(ModelNode item) {
			if (resolvedModelNodeDependency == null) {
				for (ModelNodeDependency modelNodeDependency: item.getModelNodeDependencies().values()) {
					if (modelNodeDependency != unresolvedModelNodeDependency) {
						if (modelNodeDependency.equalsByGidAidClsfr(unresolvedModelNodeDependency)) {
							if (VisitingModelNodeDependencyResolver.this.findArtifact(modelNodeDependency) != null) {
								resolvedModelNodeDependency = modelNodeDependency;
								this.clearVisitingQueue();
								break;
							}
						}
						ModelNode modelNode = this.modelNodeResult.getModelNode(modelNodeDependency.getId());
						if (modelNode != null) {
							this.enqueue(modelNode);
						}
					}
				}
			}
		}
	}

	private class TransitiveDependantsVisitor extends QueueVisitor<ModelNode> {
		private ModelNodeDependency unresolvedModelNodeDependency;
		private ModelNodeDependency resolvedModelNodeDependency;

		public ModelNodeDependency findAlternativeDependency(ModelNodeDependency unresolvedModelNodeDependency) {
			ModelNodeDependency newModelNodeDependency = null;
			this.unresolvedModelNodeDependency = unresolvedModelNodeDependency;
			this.resolvedModelNodeDependency = null;
			enqueue(this.unresolvedModelNodeDependency.getModelNode()); //enqueue the dependant of the unresolved dependency.
			this.traverse();
			if (resolvedModelNodeDependency != null) {
				Dependency newDependency = new Dependency(resolvedModelNodeDependency.getDependency());
				newModelNodeDependency = new ModelNodeDependency(unresolvedModelNodeDependency.getModelNode(), newDependency);
			}
			return newModelNodeDependency;
		}

		@Override
		protected void visit(ModelNode item) {
			if (resolvedModelNodeDependency == null) {
				for (ModelNode dependantModelNode: item.getDependants()) {
					for (ModelNodeDependency modelNodeDependency: dependantModelNode.getModelNodeDependencies().values()) {
						if (modelNodeDependency != unresolvedModelNodeDependency) {
							if (modelNodeDependency.equalsByGidAidClsfr(unresolvedModelNodeDependency)) {
								if (VisitingModelNodeDependencyResolver.this.findArtifact(modelNodeDependency) != null) {
									resolvedModelNodeDependency = modelNodeDependency;
									this.clearVisitingQueue();
									break;
								}
							}
						}
					}
					this.enqueue(dependantModelNode);
				}
			}
		}
	}
}
