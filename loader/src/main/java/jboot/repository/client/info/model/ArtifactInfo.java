package jboot.repository.client.info.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class ArtifactInfo {
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
	private String checksum;
	@XmlElement
	private Long length;

	public ArtifactInfo() {
	}

	public ArtifactInfo(String groupId, String artifactId, String version, String classifier, String type, String checksum, Long length) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.classifier = classifier;
		this.type = type;
		this.checksum = checksum;
		this.length = length;
	}

	public ArtifactInfo(ArtifactInfo artifactInfo) {
		this.groupId = artifactInfo.groupId;
		this.artifactId = artifactInfo.artifactId;
		this.version = artifactInfo.version;
		this.classifier = artifactInfo.classifier;
		this.type = artifactInfo.type;
		this.checksum = artifactInfo.checksum;
		this.length = new Long(artifactInfo.length);
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

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public Long getLength() {
		return length;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	@Override
	public String toString() {
		return getId() + ":" + classifier + ":" + type + ":" + checksum;
	}

	public String getId() {
		return groupId + ":" + artifactId + ":" + version;
	}
}
