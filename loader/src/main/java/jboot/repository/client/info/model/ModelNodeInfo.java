package jboot.repository.client.info.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ModelNodeInfo {
	@XmlElement
	private String groupId;
	@XmlElement
	private String artifactId;
	@XmlElement
	private String version;
	@XmlElement
	private ModelInfo parent;
	@XmlElement
	private List<ModelInfo> children;
	@XmlElement
	private List<DependencyInfo> dependencies;
	@XmlElement
	private String pomChecksum;
	@XmlElement
	private Long pomLength;
	@XmlElement
	private List<ArtifactInfo> artifacts;

	public ModelNodeInfo() {
	}

	public ModelNodeInfo(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.children = new ArrayList<ModelInfo>();
		this.dependencies = new ArrayList<DependencyInfo>();
		this.artifacts = new ArrayList<ArtifactInfo>();
	}

	public ModelNodeInfo(String groupId, String artifactId, String version, String pomChecksum, Long pomLength) {
		this(groupId, artifactId, version);
		this.pomChecksum = pomChecksum;
		this.pomLength = pomLength;
	}

	public ModelNodeInfo(ModelNodeInfo modelNodeInfo) {
		this.groupId = modelNodeInfo.groupId;
		this.artifactId = modelNodeInfo.artifactId;
		this.version = modelNodeInfo.version;
		this.parent = new ModelInfo(modelNodeInfo.parent);
		this.children = new ArrayList<ModelInfo>();
		if (modelNodeInfo.children != null) {
			for (ModelInfo childModelInfo : modelNodeInfo.children) {
				this.children.add(new ModelInfo(childModelInfo));
			}
		}
		this.dependencies = new ArrayList<DependencyInfo>();
		if (modelNodeInfo.dependencies != null) {
			for (DependencyInfo dependencyInfo : modelNodeInfo.dependencies) {
				this.dependencies.add(new DependencyInfo(dependencyInfo));
			}
		}
		this.artifacts = new ArrayList<ArtifactInfo>();
		if (modelNodeInfo.artifacts != null) {
			for (ArtifactInfo artifactInfo : modelNodeInfo.artifacts) {
				this.artifacts.add(new ArtifactInfo(artifactInfo));
			}
		}
		this.pomChecksum = modelNodeInfo.pomChecksum;
		this.pomLength = new Long(modelNodeInfo.pomLength);
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public ModelInfo getParent() {
		return parent;
	}

	public void setParent(ModelInfo parent) {
		this.parent = parent;
	}

	public List<ModelInfo> getChildren() {
		if (children == null) {
			children = new ArrayList<ModelInfo>();
		}
		return children;
	}

	public void setChildren(List<ModelInfo> children) {
		this.children = children;
	}

	public void addChild(ModelNodeInfo modelNodeInfo) {
		if (modelNodeInfo != null) {
			ModelInfo childInfo = new ModelInfo(modelNodeInfo.getGroupId(), modelNodeInfo.getArtifactId(), modelNodeInfo.getVersion());
			if (children == null) {
				children = new ArrayList<ModelInfo>();
			}
			children.add(childInfo);
			modelNodeInfo.setParent(new ModelInfo(groupId, artifactId, version));
		}
	}

	public List<DependencyInfo> getDependencies() {
		if (dependencies == null) {
			dependencies = new ArrayList<DependencyInfo>();
		}
		return dependencies;
	}

	public void setDependencies(List<DependencyInfo> dependencies) {
		this.dependencies = dependencies;
	}

	public void addDependency(DependencyInfo dependencyInfo) {
		if (dependencyInfo != null) {
			if (dependencies == null) {
				dependencies = new ArrayList<DependencyInfo>();
			}
			dependencies.add(dependencyInfo);
		}
	}

	public String getPomChecksum() {
		return pomChecksum;
	}

	public void setPomChecksum(String pomChecksum) {
		this.pomChecksum = pomChecksum;
	}

	public Long getPomLength() {
		return pomLength;
	}

	public void setPomLength(Long length) {
		this.pomLength = length;
	}

	public List<ArtifactInfo> getArtifacts() {
		if (artifacts == null) {
			artifacts = new ArrayList<ArtifactInfo>();
		}
		return artifacts;
	}

	public void setArtifacts(List<ArtifactInfo> artifacts) {
		this.artifacts = artifacts;
	}

	public void addArtifact(ArtifactInfo artifactInfo) {
		if (artifactInfo != null) {
			if (artifacts == null) {
				artifacts = new ArrayList<ArtifactInfo>();
			}
			artifacts.add(artifactInfo);
		}
	}

	public ArtifactInfo getArtifact(String classifier, String type) {
		List<ArtifactInfo> artifacts = getArtifacts();
		if (classifier == null) {
			classifier = "";
		}
		if (type == null || type.trim().isEmpty()) {
			type = "jar";
		}
		for (ArtifactInfo artifact : artifacts) {
			if (classifier.equals(artifact.getClassifier()) && type.equals(artifact.getType())) {
				return artifact;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "[" + this.getClass().getSimpleName() + "][Gid: " + groupId + "][Aid: " + artifactId + "][Version: " + version + "][PomCheckSum: " + pomChecksum + "][PomLength: " + pomLength + "]";
	}

	public String getId() {
		return groupId + ":" + artifactId + ":" + version;
	}
}
