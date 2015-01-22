package jboot.loader.bootstrapper.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import jboot.loader.bootstrapper.bootable.model.BootableInfoRef;
import jboot.repository.client.info.model.ModelInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "boot")
public class BootConfig implements Cloneable {

	@XmlEnum(String.class)
	public enum UpgradePolicy {
		always, never, tryandcontinue
	}

	@XmlElementWrapper(name = "localRepositories")
	@XmlElement(name = "localRepository")
	private List<String> localRepositories;

	@XmlElement
	private UpgradePolicy upgradePolicy;

	@XmlElementWrapper(name = "repositories")
	@XmlElement(name = "repository")
	private List<RepositoryInfo> repositories;

	@XmlElementWrapper(name = "dependencies")
	@XmlElement(name = "dependency")
	private List<ModelInfo> dependencies;
	
	@XmlElementWrapper(name = "excludes")
	@XmlElement(name = "exclude")
	private List<ModelInfo> excludes;

	@XmlElementWrapper(name = "customArtifacts")
	@XmlElement(name = "customArtifact")
	private List<CustomArtifact> customArtifacts;

	@XmlElementWrapper(name = "arguments")
	@XmlElement(name = "argument")
	private List<String> arguments;

	@XmlElementWrapper(name = "bootableUris")
	@XmlElement(name = "bootableUri")
	private List<String> bootableUris;
	
	@XmlElement
	private BootableInfoRef target;
	
	@XmlElement(name = "showSplash")
	private Boolean showSplash;
	
	@XmlElement(name = "splashImage")
	private String splashImage;

	public List<String> getLocalRepositories() {
		if (localRepositories == null) {
			localRepositories = new ArrayList<String>();
		}
		return localRepositories;
	}

	public void setLocalRepositories(List<String> localRepositories) {
		this.localRepositories = localRepositories;
	}

	public UpgradePolicy getUpgradePolicy() {
		return upgradePolicy;
	}

	public void setUpgradePolicy(UpgradePolicy upgradePolicy) {
		this.upgradePolicy = upgradePolicy;
	}

	public List<RepositoryInfo> getRepositories() {
		if (repositories == null) {
			repositories = new ArrayList<RepositoryInfo>();
		}
		return repositories;
	}

	public void setRepositories(List<RepositoryInfo> repositories) {
		this.repositories = repositories;
	}

	public List<ModelInfo> getDependencies() {
		if (dependencies == null) {
			dependencies = new ArrayList<ModelInfo>();
		}
		return dependencies;
	}

	public void setDependencies(List<ModelInfo> dependencies) {
		this.dependencies = dependencies;
	}
	
	public List<ModelInfo> getExcludes() {
		if (excludes == null) {
		    excludes = new ArrayList<ModelInfo>();
		}
		return excludes;
	}

	public void setExcludes(List<ModelInfo> excludes) {
		this.excludes = excludes;
	}

	public List<CustomArtifact> getCustomArtifacts() {
		if (customArtifacts == null) {
			customArtifacts = new ArrayList<CustomArtifact>();
		}
		return customArtifacts;
	}

	public void setCustomArtifacts(List<CustomArtifact> customArtifacts) {
		this.customArtifacts = customArtifacts;
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

	public List<String> getBootableUris() {
		if (bootableUris == null) {
			bootableUris = new ArrayList<String>();
		}
		return bootableUris;
	}

	public void setBootableUris(List<String> bootableUris) {
		this.bootableUris = bootableUris;
	}

	public BootableInfoRef getTarget() {
		return target;
	}

	public void setTarget(BootableInfoRef target) {
		this.target = target;
	}
	
	public Boolean isShowSplash() {
		return showSplash;
	}
	
	public void setShowSplash(Boolean showSplash) {
		this.showSplash = showSplash;
	}
	
	public String getSplashImage() {
		return splashImage;
	}
	
	public void setSplashImage(String splashImage) {
		this.splashImage = splashImage;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		BootConfig clone = (BootConfig) super.clone();
		if (localRepositories != null) {
			clone.localRepositories = new ArrayList<String>(localRepositories);
		}
		if (upgradePolicy != null) {
			clone.upgradePolicy = UpgradePolicy.valueOf(upgradePolicy.toString());
		}
		if (repositories != null) {
			clone.repositories = new ArrayList<RepositoryInfo>();
			for (RepositoryInfo repoInfo : repositories) {
				clone.repositories.add(new RepositoryInfo(repoInfo.getId(), repoInfo.getName(), repoInfo.getUrl(), repoInfo.getVersion()));
			}
		}
		if (dependencies != null) {
			clone.dependencies = new ArrayList<ModelInfo>();
			for (ModelInfo modelInfo : dependencies) {
				clone.dependencies.add(new ModelInfo(modelInfo));
			}
		}
		if (excludes != null) {
			clone.excludes = new ArrayList<ModelInfo>();
			for (ModelInfo modelInfo : excludes) {
				clone.excludes.add(new ModelInfo(modelInfo));
			}
		}
		if (customArtifacts != null) {
			clone.customArtifacts = new ArrayList<CustomArtifact>();
			for (CustomArtifact customArtifact : customArtifacts) {
				CustomArtifact clonedCustomArtifact = new CustomArtifact();
				clonedCustomArtifact.setGroupId(customArtifact.getGroupId());
				clonedCustomArtifact.setArtifactId(customArtifact.getArtifactId());
				clonedCustomArtifact.setVersion(customArtifact.getVersion());
				clonedCustomArtifact.setClassifier(customArtifact.getClassifier());
				clonedCustomArtifact.setPom(customArtifact.getPom());
				clonedCustomArtifact.setResources(new ArrayList<String>(customArtifact.getResources()));
				clone.customArtifacts.add(clonedCustomArtifact);
			}
		}
		if (arguments != null) {
			clone.arguments = new ArrayList<String>(arguments);
		}
		if (bootableUris != null) {
			clone.bootableUris = new ArrayList<String>(bootableUris);
		}
		if (target != null) {
			clone.target = new BootableInfoRef();
			clone.target.setName(target.getName());
			clone.target.setVersion(target.getVersion());
		}
		return clone;
	}
}
