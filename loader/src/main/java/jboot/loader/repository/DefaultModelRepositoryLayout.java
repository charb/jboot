package jboot.loader.boot.repository;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.node.ModelNodeArtifact;
import jboot.loader.boot.node.resource.FileResource;
import jboot.loader.boot.node.resource.Resource;

public class DefaultModelRepositoryLayout implements IModelRepositoryLayout {
	private static final String PACKAGING_POM = "pom";
	private static final String PACKAGING_JAR = "jar";
	private static final String PACKAGING_MAVENPLUGIN = "maven-plugin";
	//Maven says: The current core packaging values are: pom, jar, maven-plugin, ejb, war, ear, rar, par.

	private String rootPath;

	public DefaultModelRepositoryLayout(String rootPath) {
		if (rootPath.endsWith("/") || rootPath.endsWith("\\")) {
			this.rootPath = rootPath.substring(0, rootPath.length() - 1);
		} else {
			this.rootPath = rootPath;
		}
	}

	private String getRepositoryArtifactPath(String groupId, String artifactId, String version, String classifier, String type) {
		if (type == null || type.trim().isEmpty() || PACKAGING_MAVENPLUGIN.equals(type)) {
			type = PACKAGING_JAR;
		}
		return getRepositoryArtifactPath(groupId, artifactId, version) + File.separator + artifactId + "-" + version + ((classifier != null && !classifier.trim().isEmpty()) ? "-" + classifier : "") + "." + type;
	}

	private String getRepositoryArtifactPath(String groupId, String artifactId, String version) {
		return getRepositoryArtifactPath(groupId, artifactId) + File.separator + version;
	}

	private String getRepositoryArtifactPath(String groupId, String artifactId) {
		return rootPath + File.separator + groupId.replace('.', File.separatorChar) + File.separator + artifactId;
	}

	private File getRepositoryArtifactFile(String groupId, String artifactId, String version, String classifier, String type) {
		String path = getRepositoryArtifactPath(groupId, artifactId, version, classifier, type);
		File file = new File(path);
		if (file.exists()) {
			return file;
		}
		return null;
	}

	@Override
	public String[] getVersions(String groupId, String artifactId) {
		String path = getRepositoryArtifactPath(groupId, artifactId);
		File artifactPath = new File(path);
		List<String> versions = new ArrayList<String>();
		if (artifactPath.exists()) {
			String[] files = artifactPath.list();
			if (files != null) {
				for (String version : files) {
					File pomFile = getRepositoryArtifactFile(groupId, artifactId, version, "", PACKAGING_POM);
					if (pomFile != null) {
						versions.add(version);
					}
				}
			}
			Collections.sort(versions); //sorts from lowest to highest version.
			return versions.toArray(new String[0]);
		}
		return null;
	}

	@Override
	public FileResource getPomResource(String groupId, String artifactId, String version) {
		File pom = getRepositoryArtifactFile(groupId, artifactId, version, "", PACKAGING_POM);
		if (pom != null) {
			return new FileResource(pom);
		}
		return null;
	}

	public String getPomPath(String groupId, String artifactId, String version) {
		return getRepositoryArtifactPath(groupId, artifactId, version, "", PACKAGING_POM);
	}

	@Override
	public Resource getArtifactResource(String groupId, String artifactId, String version, String classifier, String type) {
		if (PACKAGING_POM.equals(type)) {
			return null;
		}
		File artifact = getRepositoryArtifactFile(groupId, artifactId, version, classifier, type);
		if (artifact != null) {
			return Resource.createResource(artifact);
		}
		return null;
	}

	public String getArtifactPath(String groupId, String artifactId, String version, String classifier, String type) {
		return getRepositoryArtifactPath(groupId, artifactId, version, classifier, type);
	}

	@Override
	public void fillAllArtifacts(final ModelNode modelNode) {
		String strArtifactsDirectoryPath = getRepositoryArtifactPath(modelNode.getGroupId(), modelNode.getArtifactId(), modelNode.getVersion());
		File artifactsDirectory = new File(strArtifactsDirectoryPath);
		if (artifactsDirectory.exists()) {
			FileFilter artifactFileFilter = new FileFilter() {
				private Pattern pattern = Pattern.compile(modelNode.getArtifactId() + "-" + modelNode.getVersion() + "(-\\w+)?\\.(\\w+)"); //this will also match the pom
				private Matcher matcher = pattern.matcher("");

				@Override
				public boolean accept(File pathname) {
					if (pathname.isFile()) {
						String strFilename = pathname.getName();
						if (!strFilename.endsWith(PACKAGING_POM)) {
							matcher.reset(strFilename);
							if (matcher.matches()) {
								String strClassifier = matcher.group(1);
								if (strClassifier == null) {
									strClassifier = "";
								} else {
									strClassifier = strClassifier.substring(1); //skip the dash
								}
								//String strType = matcher.group(2);
								Resource res = Resource.createResource(pathname);
								ModelNodeArtifact modelNodeArtifact = new ModelNodeArtifact(modelNode, strClassifier, res);
								modelNode.addArtifact(modelNodeArtifact);
								return true;
							}
						}
					}
					return false;
				}
			};
			artifactsDirectory.listFiles(artifactFileFilter);
		}
	}

	public String getRepositoryRootPath() {
		return rootPath;
	}

}
