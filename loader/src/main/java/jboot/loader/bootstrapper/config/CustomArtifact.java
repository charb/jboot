package jboot.loader.bootstrapper.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class CustomArtifact {

	@XmlElement(required = true)
	private String groupId;

	@XmlElement(required = true)
	private String artifactId;

	@XmlElement(required = true)
	private String version;

	@XmlElement
	private String classifier;

	@XmlElement
	private String pom;

	@XmlElementWrapper(name = "resources")
	@XmlElement(name = "resource")
	private List<String> resources;

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

	public String getPom() {
		return pom;
	}

	public void setPom(String pom) {
		this.pom = pom;
	}

	public List<String> getResources() {
		if (resources == null) {
			resources = new ArrayList<String>();
		}
		return resources;
	}

	public void setResources(List<String> resources) {
		this.resources = resources;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CustomArtifact) {
			CustomArtifact customArtifact = (CustomArtifact)obj;
			if (customArtifact.groupId.equals(groupId) &&
				customArtifact.artifactId.equals(artifactId) &&
				customArtifact.version.equals(version) &&
				(
					(
						(customArtifact.classifier == null || customArtifact.classifier.trim().isEmpty())
						&&
						(classifier == null || classifier.trim().isEmpty())
					)
					||
					(customArtifact.classifier.equals(classifier))
				)
			   ) {
				return true;
			}
		}
		return false;
	}

}
