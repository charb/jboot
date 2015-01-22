package jboot.loader.bootstrapper;

import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.bootstrapper.bootable.model.BootableInfoRef;
import jboot.loader.bootstrapper.config.BootConfig;
import jboot.loader.bootstrapper.config.CustomArtifact;
import jboot.loader.bootstrapper.config.RepositoryInfo;
import jboot.loader.bootstrapper.config.BootConfig.UpgradePolicy;
import jboot.repository.client.info.model.ModelInfo;

public class CliBootConfigBuilder {
	private static final Logger log = Logger.getLogger(CliBootConfigBuilder.class.getName());

	private BootConfig bootConfig;

	public CliBootConfigBuilder() {
		this.bootConfig = new BootConfig();
	}

	public BootConfig createBootConfig() {
		try {
			return (BootConfig)bootConfig.clone();
		} catch (CloneNotSupportedException ex) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Could not clone BootConfig", ex);
			}
		}
		return null;
	}

	public void addLocalReposisotry(String localRepository) {
		bootConfig.getLocalRepositories().add(localRepository);
	}

	public void setUpgradePolicy(String upgradePolicy) {
		bootConfig.setUpgradePolicy(UpgradePolicy.valueOf(upgradePolicy));
	}

	public void addRemoteRepository(String remoteRepository) {
		String[] repoInfo = remoteRepository.split(":", 5);
		RepositoryInfo repositoryInfo = new RepositoryInfo();
		for (int i = 0; i < repoInfo.length; i++) {
			switch (i) {
			case 0:
				repositoryInfo.setId(repoInfo[i]);
				break;
			case 1:
				repositoryInfo.setName(repoInfo[i]);
				break;
			case 2:
				repositoryInfo.setUrl("http://" + repoInfo[i]);
				break;
			case 3:
				if (!repoInfo[i].isEmpty()) {
					repositoryInfo.setUrl(repositoryInfo.getUrl() + ":" + repoInfo[i] + "/");
				} else {
					repositoryInfo.setUrl(repositoryInfo.getUrl() + "/");
				}
				break;
			case 4:
				repositoryInfo.setVersion(repoInfo[i]);
				break;
			}
		}
		bootConfig.getRepositories().add(repositoryInfo);
	}

	public void addDependency(String dependency) {
		String[] depInfo = dependency.split(":", 3);
		ModelInfo modelInfo = new ModelInfo();
		for (int i = 0; i < depInfo.length; i++) {
			switch (i) {
			case 0:
				modelInfo.setGroupId(depInfo[i]);
				break;
			case 1:
				modelInfo.setArtifactId(depInfo[i]);
				break;
			case 2:
				modelInfo.setVersion(depInfo[i]);
				break;
			}
		}
		bootConfig.getDependencies().add(modelInfo);
	}
	
	public void addExclude(String exclude) {
		String[] depInfo = exclude.split(":", 2);
		ModelInfo modelInfo = new ModelInfo();
		for (int i = 0; i < depInfo.length; i++) {
			switch (i) {
			case 0:
				modelInfo.setGroupId(depInfo[i]);
				break;
			case 1:
				modelInfo.setArtifactId(depInfo[i]);
				break;
			}
		}
		bootConfig.getExcludes().add(modelInfo);
	}

	public void addCustomArtifact(String strCustomArtifact) {
		String[] artifact = strCustomArtifact.split("::", -1);
		CustomArtifact customArtifact = new CustomArtifact();
		for (int i = 0; i < artifact.length; i++) {
			switch (i) {
			case 0:
				customArtifact.setGroupId(artifact[i]);
				break;
			case 1:
				customArtifact.setArtifactId(artifact[i]);
				break;
			case 2:
				customArtifact.setVersion(artifact[i]);
				break;
			case 3:
				customArtifact.setClassifier(artifact[i]);
				break;
			case 4:
				customArtifact.setPom(artifact[i]);
				break;
			default:
				customArtifact.getResources().add(artifact[i]);
				break;
			}
		}
		bootConfig.getCustomArtifacts().add(customArtifact);
	}

	public void addArgument(String argument) {
		bootConfig.getArguments().add(argument);
	}

	public void addBootableUri(String bootableUri) {
		bootConfig.getBootableUris().add(bootableUri);
	}

	public void setTarget(String strTarget) {
		String bootableId = strTarget;
		BootableInfoRef target = new BootableInfoRef();
		if (bootableId.indexOf(':') > -1) {
			target.setName(bootableId.substring(0, bootableId.indexOf(':')));
			target.setVersion(bootableId.substring(bootableId.indexOf(':') + 1));
		} else {
			target.setName(bootableId);
		}
		bootConfig.setTarget(target);
	}
	
	public void setShowSplash(Boolean showSplash) {
		bootConfig.setShowSplash(showSplash);
	}

	public void setSplashImage(String splashImage) {
		bootConfig.setSplashImage(splashImage);
	}
}
