package jboot.repository.client.info.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dependency")
public class DependencyInfo {
	@XmlElement
	private String groupId;
	@XmlElement
	private String artifactId;
	@XmlElement
	private String version;
	@XmlElement
	private String classifier;
	@XmlElement
	private String type;
	@XmlElement
	private String scope;

	public DependencyInfo() {
	}

	public DependencyInfo(String groupId, String artifactId, String version, String classifier, String type, String scope) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.classifier = classifier;
		this.type = type;
		this.scope = scope;
	}

	public DependencyInfo(DependencyInfo dependencyInfo) {
		this.groupId = dependencyInfo.groupId;
		this.artifactId = dependencyInfo.artifactId;
		this.version = dependencyInfo.version;
		this.classifier = dependencyInfo.classifier;
		this.type = dependencyInfo.type;		
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

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String toString() {
		return "[" + this.getClass().getSimpleName() + "][Gid: " + groupId + "][Aid: " + artifactId + "][Version: " + version + "][Classifier: " + classifier + "][Type: " + type + "][Scope: " + scope + "]";
	}

	public String getId() {
		return groupId + ":" + artifactId + ":" + version;
	}
}
