package jboot.loader.bootstrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.bootstrapper.bootable.BootableLauncher;
import jboot.loader.bootstrapper.bootable.model.BootableInfo;
import jboot.loader.bootstrapper.config.BootConfig;
import jboot.loader.bootstrapper.splash.ISplashScreen;

public class Bootstrapper {
	private static final Logger bootstrapperLog = Logger.getLogger("jboot.bootstrapper");

	private BootConfig bootConfig;
	private String[] bootConfigArgs;
	private IBootstrapLoader bootstrapLoader;
	private ISplashScreen splashScreen;
	private BootableLauncher bootableLauncher;

	public Bootstrapper(BootConfig bootConfig, ISplashScreen splashScreen) throws Exception {
		if (bootConfig == null) {
			throw new NullPointerException("Cannot initialize a Bootstrapper with a null BootConfig.");
		}
		this.bootConfig = bootConfig;
		this.splashScreen = splashScreen;
		initBootstrapLoader();
	}
	
	public Bootstrapper(BootConfig bootConfig) throws Exception {
		if (bootConfig == null) {
			throw new NullPointerException("Cannot initialize a Bootstrapper with a null BootConfig.");
		}
		this.bootConfig = bootConfig;
		initBootstrapLoader();
	}

	private void initBootstrapLoader() throws Exception {
		bootstrapLoader = DefaultBootstrapLoader.createDefaultBootstrapLoader(bootConfig, splashScreen);
	}

	private BootableLauncher getBootableLauncher() throws Exception {
		if (bootableLauncher == null) {
			bootableLauncher = new BootableLauncher(bootstrapLoader.getBootClassLoader(), bootConfig.getBootableUris());
			if (bootConfigArgs != null) {
				bootableLauncher.setBootConfigArgs(bootConfigArgs);
			}
		}
		return bootableLauncher;
	}

	public void printBootablesHelp() throws Exception {
		getBootableLauncher().printHelp();
	}

	public void bootstrap(String args[]) throws Exception {
		if (bootConfig.getTarget() != null) {
			bootstrap(bootConfig.getTarget().getName(), bootConfig.getTarget().getVersion(), args);
		} else {
			throw new NullPointerException("No bootable target specified in the boot configuration.");
		}
	}

	public void bootstrap(String bootableName, String bootableVersion, String args[]) throws Exception {
		if(splashScreen != null){
			splashScreen.setMessage("Initializing bootable launcher...");
			splashScreen.incrementProgress(10);
		}
		BootableLauncher bootableLauncher = getBootableLauncher();
		if(splashScreen != null){
			splashScreen.clearMessage();
		}
		BootableInfo bootable = null;
		if (bootableVersion == null || bootableVersion.trim().isEmpty()) {
			bootable = bootableLauncher.getLatestBootable(bootableName);
		} else {
			bootable = bootableLauncher.getBootable(bootableName, bootableVersion);
		}
		if (bootable == null) {
			throw new NullPointerException("The specified bootable target " + bootableName + ":" + bootableVersion + " does not exist.");
		} else if (bootable.isAbstractBootable()) {
			throw new Exception("The specified bootable target " + bootableName + ":" + bootableVersion + " is abstract.");
		}

		bootable = bootableLauncher.inheritParents(bootable);

		List<String> argsList = new ArrayList<String>(bootable.getArguments().size() + bootConfig.getArguments().size() + args.length);
		argsList.addAll(bootable.getArguments());
		argsList.addAll(bootConfig.getArguments());
		argsList.addAll(Arrays.asList(args));
		String[] effectiveArgs = argsList.toArray(new String[0]); 

				
		if(splashScreen != null){
			splashScreen.setMessage("Launching "+ bootableName +"...");
			splashScreen.incrementProgress(10);
		}
		
		if(splashScreen != null) {
			splashScreen.removeSplashScreen();
			splashScreen = null;
		}
		
		bootableLauncher.launch(bootable, effectiveArgs);
	}

	public void dumpClassLoader(OutputStream os) throws IOException {
		bootstrapLoader.getBootClassLoader().dumpClassLoader(os);
	}

	public String[] getBootConfigArgs() {
		return bootConfigArgs;
	}

	public void setBootConfigArgs(String[] bootConfigArgs) {
		this.bootConfigArgs = bootConfigArgs;
	}
}
