package jboot.loader.boot.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelRepositoryLayoutFactory {
	private List<String> repositoryRootPaths;
	// gid:aid -> [ver -> pom-path]
	private Map<String, Map<String, String>> poms;
	// gid:aid -> [ver -> [classifier -> list of paths]]
	private Map<String, Map<String, Map<String, List<String>>>> artifacts;

	private int customPomCount;
	private int customArtifactCount;

	public ModelRepositoryLayoutFactory() {
		repositoryRootPaths = new ArrayList<String>();
		poms = new HashMap<String, Map<String, String>>();
		artifacts = new HashMap<String, Map<String, Map<String, List<String>>>>();
	}

	public ModelRepositoryLayoutFactory addRepositoryRootPath(String rootPath) {
		if (!repositoryRootPaths.contains(rootPath)) {
			repositoryRootPaths.add(rootPath);
		}
		return this;
	}

	public ModelRepositoryLayoutFactory removeRepositoryRootPath(String rootPath) {
		repositoryRootPaths.remove(rootPath);
		return this;
	}

	public ModelRepositoryLayoutFactory addPom(String groupId, String artifactId, String version, String pomPath) {
		Map<String, String> pomVersions = poms.get(groupId + ":" + artifactId);
		if (pomVersions == null) {
			pomVersions = new HashMap<String, String>();
			poms.put(groupId + ":" + artifactId, pomVersions);
		}
		pomVersions.put(version, pomPath);
		customPomCount++;
		return this;
	}

	public ModelRepositoryLayoutFactory removePom(String groupId, String artifactId, String version) {
		Map<String, String> pomVersions = poms.get(groupId + ":" + artifactId);
		if (pomVersions != null) {
			pomVersions.remove(version);
			customPomCount--;
		}
		return this;
	}

	public ModelRepositoryLayoutFactory addArtifact(String groupId, String artifactId, String version, String classifier, List<String> artifactPaths) {
		if (classifier == null) {
			classifier = "";
		}
		Map<String, Map<String, List<String>>> artifactVersions = artifacts.get(groupId + ":" + artifactId);
		if (artifactVersions == null) {
			artifactVersions = new HashMap<String, Map<String,List<String>>>();
			artifacts.put(groupId + ":" + artifactId, artifactVersions);
		}
		Map<String, List<String>> artifactClassifiers = artifactVersions.get(version);
		if (artifactClassifiers == null) {
			artifactClassifiers = new HashMap<String, List<String>>();
			artifactVersions.put(version, artifactClassifiers);
		}
		artifactClassifiers.put(classifier, artifactPaths);
		customArtifactCount++;
		return this;
	}

	public ModelRepositoryLayoutFactory removeArtifact(String groupId, String artifactId, String version, String classifier) {
		if (classifier == null) {
			classifier = "";
		}
		Map<String, Map<String, List<String>>> artifactVersions = artifacts.get(groupId + ":" + artifactId);
		if (artifactVersions != null) {
			Map<String, List<String>> artifactClassifiers = artifactVersions.get(version);
			if (artifactClassifiers != null) {
				artifactClassifiers.remove(classifier);
				customArtifactCount--;
			}
		}
		return this;
	}

	public IModelRepositoryLayout createModelRepositoryLayout() {
		IModelRepositoryLayout defaultRepository = createDefaultModelRepositoryLayout();
		IModelRepositoryLayout customRepository = createCustomModelRepositoryLayout();
		if (defaultRepository != null && customRepository != null) {
			return new MultiModelRepositoryLayout(customRepository, defaultRepository); //the customRepository comes before the default repository, in order for its entries to override the default repo.
		} else if (defaultRepository != null) {
			return defaultRepository;
		} else if (customRepository != null) {
			return customRepository;
		}
		return null; //no rootpaths and/or custom poms/artifacts have been added in the factory. 
	}

	private IModelRepositoryLayout createDefaultModelRepositoryLayout() {
		if (repositoryRootPaths.size() == 1) {
			return new DefaultModelRepositoryLayout(repositoryRootPaths.get(0));
		} else if (repositoryRootPaths.size() > 1) {
			DefaultModelRepositoryLayout[] defaultModelRepositoryLayouts = new DefaultModelRepositoryLayout[repositoryRootPaths.size()];
			int i = 0;
			for (String rootPath : repositoryRootPaths) {
				defaultModelRepositoryLayouts[i] = new DefaultModelRepositoryLayout(rootPath);
				i++;
			}
			return new MultiModelRepositoryLayout(defaultModelRepositoryLayouts);
		}
		return null;
	}

	private IModelRepositoryLayout createCustomModelRepositoryLayout() {
		if (customPomCount > 0 || customArtifactCount > 0) {
			return new CustomModelRepositoryLayout(poms, artifacts);
		}
		return null;
	}

}
