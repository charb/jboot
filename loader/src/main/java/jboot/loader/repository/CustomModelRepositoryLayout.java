package jboot.loader.boot.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.node.ModelNodeArtifact;
import jboot.loader.boot.node.resource.FileResource;
import jboot.loader.boot.node.resource.Resource;

public class CustomModelRepositoryLayout implements IModelRepositoryLayout {
	//private static final Logger log = Logger.getLogger(CustomModelRepositoryLayout.class.getName());

	// gid:aid -> [ver -> pom-path]
	private Map<String, Map<String, String>> poms;

	// gid:aid -> [ver -> [classifier -> list of paths]]
	private Map<String, Map<String, Map<String, List<String>>>> artifacts;

	public CustomModelRepositoryLayout(Map<String, Map<String, String>> poms, Map<String, Map<String, Map<String, List<String>>>> artifacts) {
		this.poms = poms;
		this.artifacts = artifacts;
	}

	@Override
	public void fillAllArtifacts(ModelNode modelNode) {
		Map<String, Map<String, List<String>>> artifactVersions = artifacts.get(modelNode.getGroupId() + ":" + modelNode.getArtifactId());
		if (artifactVersions != null) {
			Map<String, List<String>> artifactClassifiers = artifactVersions.get(modelNode.getVersion());
			if (artifactClassifiers != null) {
				for (Map.Entry<String, List<String>> entry : artifactClassifiers.entrySet()) {
					String classifier = entry.getKey(); 
					List<String> artifactPaths = entry.getValue();
					if (artifactPaths != null && artifactPaths.size() > 0) {
						Resource res = getResourceFromPaths(artifactPaths);
						ModelNodeArtifact modelNodeArtifact = new ModelNodeArtifact(modelNode, classifier, res);
						modelNode.addArtifact(modelNodeArtifact);
					}
				}
			}
		}
	}

	@Override
	public Resource getArtifactResource(String groupId, String artifactId, String version, String classifier, String type) {
		List<String> artifactPaths = getArtifactPaths(groupId, artifactId, version, classifier, type);
		if (artifactPaths != null) {
			return getResourceFromPaths(artifactPaths);
		}
		return null;
	}

	public List<String> getArtifactPaths(String groupId, String artifactId, String version, String classifier, String type) {
		if (classifier == null) {
			classifier = "";
		}
		Map<String, Map<String, List<String>>> artifactVersions = artifacts.get(groupId + ":" + artifactId);
		if (artifactVersions != null) {
			Map<String, List<String>> artifactClassifiers = artifactVersions.get(version);
			if (artifactClassifiers != null) {
				List<String> artifactPaths = artifactClassifiers.get(classifier);
				if (artifactPaths != null && artifactPaths.size() > 0) {
					return artifactPaths;
				}
			}
		}
		return null;
	}

	@Override
	public FileResource getPomResource(String groupId, String artifactId, String version) {
		String pomPath = getPomPath(groupId, artifactId, version);
		if (pomPath != null) {
			File f = new File(pomPath);
			if (f.exists()) {
				return new FileResource(f);
			} else {
				return null;
			}
		}
		return null;
	}

	public String getPomPath(String groupId, String artifactId, String version) {
		Map<String, String> pomVersions = poms.get(groupId + ":" + artifactId);
		if (pomVersions != null) {
			return pomVersions.get(version);
		}
		return null;
	}

	@Override
	public String[] getVersions(String groupId, String artifactId) {
		Map<String, String> pomVersions = poms.get(groupId + ":" + artifactId);
		if (pomVersions != null) {
			List<String> versions = new ArrayList<String>(pomVersions.keySet());
			Collections.sort(versions); //sort alphabetically.
			return versions.toArray(new String[0]);
		}
		return null;
	}

	private Resource getResourceFromPaths(List<String> paths) {
		File[] files = new File[paths.size()];
		int i = 0;
		for (String path : paths) {
			files[i] = new File(path);
			i++;
		}
		return Resource.createResource(files);
	}
}
