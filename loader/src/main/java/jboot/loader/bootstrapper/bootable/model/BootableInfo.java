package jboot.loader.bootstrapper.bootable.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class BootableInfo {
	@XmlElement(name = "parent")
	private BootableInfoRef parentRef;

	@XmlTransient
	private BootableInfo parent;

	@XmlElement(name = "abstract", defaultValue = "false")
	private boolean abstractBootable;

	@XmlElement
	private String name;

	@XmlElement
	private String version;

	@XmlElement
	private String description;

	@XmlElementWrapper(name = "argumentInfos")
	@XmlElement(name = "argumentInfo")
	private List<ArgumentInfo> argumentInfos;

	@XmlElementWrapper(name = "arguments")
	@XmlElement(name = "argument")
	private List<String> arguments;

	@XmlElementWrapper(name = "pre", nillable = true, required = false)
	@XmlElement(name = "bootableRef")
	private List<BootableInfoRef> preBootables;

	@XmlElementWrapper(name = "post", nillable = true, required = false)
	@XmlElement(name = "bootableRef")
	private List<BootableInfoRef> postBootables;

	public BootableInfoRef getParentRef() {
		return parentRef;
	}

	public void setParentRef(BootableInfoRef parentRef) {
		this.parentRef = parentRef;
	}

	public BootableInfo getParent() {
		return parent;
	}

	public void setParent(BootableInfo parent) {
		this.parent = parent;
	}

	public boolean isAbstractBootable() {
		return abstractBootable;
	}

	public void setAbstractBootable(boolean abstractBootable) {
		this.abstractBootable = abstractBootable;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ArgumentInfo> getArgumentInfos() {
		if (argumentInfos == null) {
			argumentInfos = new ArrayList<ArgumentInfo>();
		}
		return argumentInfos;
	}

	public void setArgumentInfos(List<ArgumentInfo> argumentInfos) {
		this.argumentInfos = argumentInfos;
	}

	public List<String> getArguments() {
		if (arguments == null) {
			arguments = new ArrayList<String>();
		}
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

	public List<BootableInfoRef> getPreBootables() {
		if (preBootables == null) {
			preBootables = new ArrayList<BootableInfoRef>();
		}
		return preBootables;
	}

	public void setPreBootables(List<BootableInfoRef> preBootables) {
		this.preBootables = preBootables;
	}

	public List<BootableInfoRef> getPostBootables() {
		if (postBootables == null) {
			postBootables = new ArrayList<BootableInfoRef>();
		}
		return postBootables;
	}

	public void setPostBootables(List<BootableInfoRef> postBootables) {
		this.postBootables = postBootables;
	}

	
}
