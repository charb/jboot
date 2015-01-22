package jboot.repository.client.info.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class ModelInfo {
    @XmlElement
    private String groupId;
    @XmlElement
    private String artifactId;
    @XmlElement
    private String version;

    public ModelInfo() {
	}

    public ModelInfo(String groupId, String artifactId, String version) {
    	this.groupId = groupId;
    	this.artifactId = artifactId;
    	this.version = version;
	}

    public ModelInfo(ModelInfo modelInfo) {
    	this.groupId = modelInfo.groupId;
    	this.artifactId = modelInfo.artifactId;
    	this.version = modelInfo.version;
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

	public String getId() {
		return groupId + ":" + artifactId + ":" + version;
	}

}
