package jboot.loader.bootstrapper.bootable;

import java.util.Set;

import jboot.loader.bootstrapper.bootable.model.BootableInfo;

public abstract class AbstractBootableLauncher<T extends BootableInfo> {
	private ClassLoader classLoader;
	private String[] bootConfigArgs;

	public AbstractBootableLauncher(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public String[] getBootConfigArgs() {
		return bootConfigArgs;
	}

	public void setBootConfigArgs(String[] bootConfigArgs) {
		this.bootConfigArgs = bootConfigArgs;
	}

	protected abstract T inheritParents(T bootable, Set<BootableInfo> inherited) throws Exception;
	public abstract void launch(T bootable, String args[]) throws Exception;
}
