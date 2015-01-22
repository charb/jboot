package jboot.loader.bootstrapper.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class RepositoryInfo {
	@XmlElement(required = true)
    private String id;

	@XmlElement
    private String name;

    @XmlElement(required = true)
    private String url;

    @XmlElement
    private String version;

    public RepositoryInfo() {
    }

    public RepositoryInfo(String id, String name, String url, String version) {
    	this.id = id;
        this.name = name;
        this.url = url;
        this.version = version;
    }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String layout) {
		this.version = layout;
	}

}
