package jboot.loader.bootstrapper.classloader;

import java.util.ArrayList;
import java.util.List;

import jboot.loader.bootstrapper.DefaultBootstrapLoader;
import jboot.loader.bootstrapper.IBootstrapLoader;
import jboot.loader.bootstrapper.config.BootConfig.UpgradePolicy;
import jboot.repository.client.info.model.ModelInfo;

public class BootClassLoaderBuilder {
	private List<ModelInfo> models;
	private List<ModelInfo> excludes;
	private UpgradePolicy upgradePolicy;
	private ClassLoader parent;

	public BootClassLoaderBuilder() {
		models = new ArrayList<ModelInfo>(0);
		excludes = new ArrayList<ModelInfo>(0);
	}

	public List<ModelInfo> getModels() {
		return models;
	}
	
	public List<ModelInfo> getExcludes() {
		return excludes;
	}

	public BootClassLoaderBuilder addDependency(String groupId, String artifactId, String version) {
		return addDependency(new ModelInfo(groupId, artifactId, version));
	}

	public BootClassLoaderBuilder addDependency(ModelInfo modelInfo) {
		if (modelInfo != null) {
			models.add(modelInfo);
		}
		return this;
	}
	
	public BootClassLoaderBuilder addExclude(ModelInfo modelInfo) {
		if (modelInfo != null) {
		    excludes.add(modelInfo);
		}
		return this;
	}

	public UpgradePolicy getUpgradePolicy() {
		return upgradePolicy;
	}

	public void setUpgradePolicy(UpgradePolicy upgradePolicy) {
		this.upgradePolicy = upgradePolicy;
	}

	public ClassLoader getParent() {
		return parent;
	}

	public void setParent(ClassLoader parent) {
		this.parent = parent;
	}

	public BootClassLoader createBootClassLoader() throws Exception {
		IBootstrapLoader bootstrapLoader = null;
		BootClassLoader parent = null;
		if (this.parent instanceof BootClassLoader) {
			parent = (BootClassLoader) this.parent;
			bootstrapLoader = parent.getBootstrapLoader();
		} else if (this.parent == null && Thread.currentThread().getContextClassLoader() instanceof BootClassLoader) {
			parent = (BootClassLoader) Thread.currentThread().getContextClassLoader();
			bootstrapLoader = parent.getBootstrapLoader();
		} else {
			bootstrapLoader = DefaultBootstrapLoader.getDefaultBootstrapLoader();
		}

		return bootstrapLoader.createBootClassLoader(parent, models, excludes, upgradePolicy);
	}

}
