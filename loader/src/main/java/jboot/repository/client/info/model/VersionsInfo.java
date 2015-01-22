package jboot.repository.client.info.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class VersionsInfo {
	@XmlElement
	private List<String> versions;

	public VersionsInfo() {
	}

	public VersionsInfo(String[] versions) {
		this.versions = new ArrayList<String>();
		for (String version : versions) {
			this.versions.add(version);
		}
	}

	public List<String> getVersions() {
		if (versions == null) {
			versions = new ArrayList<String>();
		}
		return versions;
	}

	public void setVersions(List<String> versions) {
		this.versions = versions;
	}
}
