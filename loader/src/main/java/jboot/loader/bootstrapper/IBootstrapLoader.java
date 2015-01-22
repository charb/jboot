package jboot.loader.bootstrapper;

import java.util.List;

import jboot.loader.bootstrapper.classloader.BootClassLoader;
import jboot.loader.bootstrapper.config.BootConfig.UpgradePolicy;
import jboot.repository.client.info.model.ModelInfo;

public interface IBootstrapLoader {
	public BootClassLoader getBootClassLoader();
	public BootClassLoader createBootClassLoader(ClassLoader parent, List<ModelInfo> models, List<ModelInfo> excludes, UpgradePolicy upgradePolicy) throws Exception;
}
