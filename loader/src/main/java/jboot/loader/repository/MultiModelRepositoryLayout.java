package jboot.loader.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jboot.loader.node.ModelNode;
import jboot.loader.node.resource.FileResource;
import jboot.loader.node.resource.Resource;

public class MultiModelRepositoryLayout implements IModelRepositoryLayout {

	private List<IModelRepositoryLayout> repositoryLayouts;

	public MultiModelRepositoryLayout() {
		this.repositoryLayouts = new ArrayList<IModelRepositoryLayout>();
	}

	public MultiModelRepositoryLayout(IModelRepositoryLayout... repositoryLayouts) {
		this.repositoryLayouts = Arrays.asList(repositoryLayouts);
	}

	public List<IModelRepositoryLayout> getRepositoryLayouts() {
		return Collections.unmodifiableList(repositoryLayouts);
	}

	/**
	 * This method has a cumulative effect. However artifacts having the same classifier are overriden by the earliest occurence of the artifact in the list of layouts.
	 * 
	 */
	@Override
	public void fillAllArtifacts(ModelNode modelNode) {
		for (int i = repositoryLayouts.size()-1; i >= 0; i--) { //in reverse order (so that layouts placed in the begining of the list win over the latter ones).
			IModelRepositoryLayout layout = repositoryLayouts.get(i);
			layout.fillAllArtifacts(modelNode);
		}
	}

	@Override
	public Resource getArtifactResource(String groupId, String artifactId, String version, String classifier, String type) {
		for (IModelRepositoryLayout layout : repositoryLayouts) {
			Resource resource = layout.getArtifactResource(groupId, artifactId, version, classifier, type);
			if (resource != null) {
				return resource;
			}
		}
		return null;
	}

	@Override
	public FileResource getPomResource(String groupId, String artifactId, String version) {
		for (IModelRepositoryLayout layout : repositoryLayouts) {
			FileResource fileResource = layout.getPomResource(groupId, artifactId, version);
			if (fileResource != null) {
				return fileResource;
			}
		}
		return null;
	}

	/**
	 * This method has a cumulative effect over all layouts.
	 * 
	 */
	@Override
	public String[] getVersions(String groupId, String artifactId) {
		Set<String> versionsSet = new TreeSet<String>(); //version are ordered using alphabetical order for now...
		for (IModelRepositoryLayout layout : repositoryLayouts) {
			String[] versions = layout.getVersions(groupId, artifactId);
			if (versions != null) {
				versionsSet.addAll(Arrays.asList(versions));
			}
		}
		return versionsSet.toArray(new String[0]);
	}

}
