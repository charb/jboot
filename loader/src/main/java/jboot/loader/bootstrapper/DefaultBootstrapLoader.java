package jboot.loader.bootstrapper;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jboot.loader.boot.node.DefaultModelLoader;
import jboot.loader.boot.node.IModelLoader;
import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.repository.DefaultModelRepositoryLayout;
import jboot.loader.boot.repository.IModelRepositoryLayout;
import jboot.loader.boot.repository.ModelRepositoryLayoutFactory;
import jboot.loader.boot.resolver.DefaultProjectResolver;
import jboot.loader.boot.resolver.ExceptionCollector;
import jboot.loader.boot.resolver.IProjectResolver;
import jboot.loader.boot.resolver.ModelNodeResult;
import jboot.loader.bootstrapper.classloader.BootClassLoader;
import jboot.loader.bootstrapper.config.BootConfig;
import jboot.loader.bootstrapper.config.CustomArtifact;
import jboot.loader.bootstrapper.config.RepositoryInfo;
import jboot.loader.bootstrapper.config.BootConfig.UpgradePolicy;
import jboot.loader.bootstrapper.splash.ISplashScreen;
import jboot.loader.upgrader.Upgrader;
import jboot.repository.client.IRepository;
import jboot.repository.client.RepositoryAccess;
import jboot.repository.client.info.model.ModelInfo;
import jboot.repository.client.info.model.VersionsInfo;
import jboot.repository.client.protocol.URLStreamHandlerFactoryImpl;

public class DefaultBootstrapLoader implements IBootstrapLoader {
	private static final Logger bootstrapperLog = Logger.getLogger("jboot.bootstrapper");
	private static final String BOOTSTRAPPER_PROPERTIES = "jboot/loader/bootstrapper.properties";
	private static final String LOCAL_RESOURCES_ENDWITH = "local.resources.endwith";
	private static final String LOCAL_CLASSES_ENDWITH = "local.classes.endwith";
	private static final String JBOOT_PRUNEMODELS = "jboot.pruneModels";

	private static DefaultBootstrapLoader thisLoader;

	private BootConfig bootConfig;
	private IRepository remoteRepository;
	private IModelRepositoryLayout localRepository;
	private ModelNodeResult modelNodeResult;
	private BootClassLoader initialClassLoader;
	private Set<String> localResources;
	private Set<String> localClasses;
	private ISplashScreen splashScreen;

	public static DefaultBootstrapLoader createDefaultBootstrapLoader(BootConfig bootConfig, ISplashScreen splashScreen) throws Exception {
		if (thisLoader == null) {
			thisLoader = new DefaultBootstrapLoader(bootConfig, splashScreen);
			thisLoader.initClassLoader(bootConfig.getDependencies(), bootConfig.getExcludes(), bootConfig.getUpgradePolicy());
		} else {
			throw new Exception("An instance of " + DefaultBootstrapLoader.class.getSimpleName() + " is already created.");
		}
		
		return thisLoader;
	}
	
	public static DefaultBootstrapLoader createDefaultBootstrapLoader(BootConfig bootConfig) throws Exception {
		if (thisLoader == null) {
			thisLoader = new DefaultBootstrapLoader(bootConfig);
			thisLoader.initClassLoader(bootConfig.getDependencies(), bootConfig.getExcludes(), bootConfig.getUpgradePolicy());
		} else {
			throw new Exception("An instance of " + DefaultBootstrapLoader.class.getSimpleName() + " is already created.");
		}
		
		return thisLoader;
	}

	public static DefaultBootstrapLoader getDefaultBootstrapLoader() {
		return thisLoader;
	}

	protected DefaultBootstrapLoader(BootConfig bootConfig, ISplashScreen splashScreen) throws Exception {
		this(bootConfig);
		this.splashScreen = splashScreen;
	}
	
	protected DefaultBootstrapLoader(BootConfig bootConfig) throws Exception {
		this.bootConfig = bootConfig;
		List<URL> repositoryUrls = new ArrayList<URL>();
		for (RepositoryInfo repositoryInfo : bootConfig.getRepositories()) {
			repositoryUrls.add(new URL(repositoryInfo.getUrl())); //TODO: pass the version of each repo to the RepositoryAccess.
		}
		remoteRepository = new RepositoryAccess(repositoryUrls);
		URL.setURLStreamHandlerFactory(new URLStreamHandlerFactoryImpl(remoteRepository));
		initLocalRepository(this.bootConfig.getLocalRepositories(), this.bootConfig.getCustomArtifacts());
		modelNodeResult = new ModelNodeResult();
		initFromProperties();
	}

	private void initFromProperties() throws Exception {
		InputStream is = this.getClass().getResourceAsStream(BOOTSTRAPPER_PROPERTIES);
		if (is != null) {
			Properties p = new Properties();
			try {
				p.load(is);
			} finally {
				is.close();
			}
			String strLocalResources = p.getProperty(LOCAL_RESOURCES_ENDWITH);
			if (strLocalResources != null && !strLocalResources.isEmpty()) {
				String[] localResources = strLocalResources.split(",");
				if (localResources != null && localResources.length > 0) {
					this.localResources = new HashSet<String>();
					for (String str : localResources) {
						str = str.trim();
						if (!str.isEmpty()) {
							this.localResources.add(str);
						}
					}
				}
			}
			String strLocalClasses = p.getProperty(LOCAL_CLASSES_ENDWITH);
			if (strLocalClasses != null && !strLocalClasses.isEmpty()) {
				String[] localClasses = strLocalClasses.split(",");
				if (localClasses != null && localClasses.length > 0) {
					this.localClasses = new HashSet<String>();
					for (String str : localClasses) {
						str = str.trim();
						if (!str.isEmpty()) {
							this.localClasses.add(str);
						}
					}
				}
			}
		}
	}

	private void initClassLoader(List<ModelInfo> dependencies, List<ModelInfo> excludes, UpgradePolicy upgradePolicy) throws Exception {
		if (initialClassLoader == null) {
			initialClassLoader = createBootClassLoader(null, dependencies, excludes, upgradePolicy);
			Thread.currentThread().setContextClassLoader(initialClassLoader);
			if (System.getProperty(JBOOT_PRUNEMODELS, "true").equalsIgnoreCase("true")) {
			    pruneModelNodes();
			}
		}
	}

	private void initLocalRepository(List<String> localRepositories, List<CustomArtifact> customArtifacts) {
		ModelRepositoryLayoutFactory factory = new ModelRepositoryLayoutFactory();
		for (String localRepoPath : localRepositories) {
			factory.addRepositoryRootPath(localRepoPath);
		}
		if (customArtifacts != null) {
			for (CustomArtifact customArtifact : customArtifacts) {
				if (customArtifact.getPom() != null && !customArtifact.getPom().trim().isEmpty()) {
					factory.addPom(customArtifact.getGroupId(), customArtifact.getArtifactId(), customArtifact.getVersion(), customArtifact.getPom());
				}
				if (customArtifact.getResources() != null && customArtifact.getResources().size() > 0) {
					factory.addArtifact(customArtifact.getGroupId(), customArtifact.getArtifactId(), customArtifact.getVersion(), customArtifact.getClassifier(), customArtifact.getResources());
				}
			}
		}
		localRepository = factory.createModelRepositoryLayout();
	}

	private void resolveModelVersions(List<ModelInfo> models, UpgradePolicy upgradePolicy) throws Exception {
		for (ModelInfo modelInfo : models) {
			if (modelInfo.getVersion() == null || modelInfo.getVersion().trim().isEmpty()) {
				String[] versions = null;
				if (upgradePolicy.equals(UpgradePolicy.never)) {
					versions = localRepository.getVersions(modelInfo.getGroupId(), modelInfo.getArtifactId());
				} else {
					try {
						VersionsInfo versionsInfo = remoteRepository.getVersionsInfo(modelInfo.getGroupId(), modelInfo.getArtifactId());
						versions = versionsInfo.getVersions().toArray(new String[0]);
					} catch (Exception ex) {
						String strMsg = "Remote repository failed to return a versions list for " + modelInfo.getGroupId() + ":" + modelInfo.getArtifactId();
						if (!upgradePolicy.equals(UpgradePolicy.tryandcontinue)) {
							throw new Exception(strMsg, ex);
						}
						if (bootstrapperLog.isLoggable(Level.WARNING)) {
							bootstrapperLog.log(Level.WARNING, strMsg + ". Using local repository to resolve version of " + modelInfo.getGroupId() + ":" + modelInfo.getArtifactId(), ex);
						}
						versions = localRepository.getVersions(modelInfo.getGroupId(), modelInfo.getArtifactId());
					}
				}
				if (versions != null && versions.length > 0) {
					modelInfo.setVersion(versions[versions.length-1]);
					if (bootstrapperLog.isLoggable(Level.INFO)) {
						bootstrapperLog.log(Level.INFO, "Using version " + modelInfo.getVersion() + " for dependency " + modelInfo.getGroupId() + ":" + modelInfo.getArtifactId());
					}
				} else {
					if (bootstrapperLog.isLoggable(Level.WARNING)) {
						bootstrapperLog.log(Level.WARNING, "Could not determine version of dependency " + modelInfo.getGroupId() + ":" + modelInfo.getArtifactId());
					}
				}
			}
		}
	}

	private void upgrade(List<ModelInfo> models, UpgradePolicy upgradePolicy) throws Exception {
		if (upgradePolicy == null || upgradePolicy.equals(UpgradePolicy.never)) {
			return;
		}
		if (!(localRepository instanceof DefaultModelRepositoryLayout)) {
			String strMsg = "Cannot upgrade local repository. Only a root path based DefaultModelRepositoryLayout can be upgraded. Current local repository is a " + localRepository.getClass().getName();
			if (!upgradePolicy.equals(UpgradePolicy.tryandcontinue)) {
				throw new Exception(strMsg);
			}
			if (bootstrapperLog.isLoggable(Level.WARNING)) {
				bootstrapperLog.log(Level.WARNING, strMsg);
			}
			return;
		}
		try {
			remoteRepository.ping();
		} catch (Exception ex) {
			String strMsg = "Cannot upgrade local repository. Remote repository not reacheable.";
			if (!upgradePolicy.equals(UpgradePolicy.tryandcontinue)) {
				throw new Exception(strMsg, ex);
			}
			if (bootstrapperLog.isLoggable(Level.WARNING)) {
				bootstrapperLog.log(Level.WARNING, strMsg, ex);
			}
			return;
		}
		Upgrader upgrader = new Upgrader(remoteRepository, (DefaultModelRepositoryLayout)localRepository);
		for (ModelInfo modelInfo : models) {
			upgrader.addProject(modelInfo.getGroupId(), modelInfo.getArtifactId(), modelInfo.getVersion());
		}
		try {
			upgrader.upgrade();
		} catch (Exception ex) {
			String strMsg = "Upgrade failed.";
			if (!upgradePolicy.equals(UpgradePolicy.tryandcontinue)) {
				throw new Exception(strMsg, ex);
			}
			if (bootstrapperLog.isLoggable(Level.WARNING)) {
				bootstrapperLog.log(Level.WARNING, strMsg, ex);
			}
		}
	}

	private void resolve(List<ModelInfo> models) throws Exception {
	    IModelLoader modelLoader = new DefaultModelLoader();
		IProjectResolver projectResolver = new DefaultProjectResolver(modelNodeResult, localRepository, modelLoader);
		for (ModelInfo modelInfo : models) {
			ExceptionCollector collector = projectResolver.resolve(modelInfo.getGroupId(), modelInfo.getArtifactId(), modelInfo.getVersion());
			collector.log();
		}
	}

	public BootConfig getBootConfiguration() {
		return bootConfig;
	}

	public IRepository getRemoteRepository() {
		return remoteRepository;
	}

	@Override
	public BootClassLoader getBootClassLoader() {
		return initialClassLoader;
	}

	@Override
	public BootClassLoader createBootClassLoader(ClassLoader parent, List<ModelInfo> models, List<ModelInfo> excludes, UpgradePolicy upgradePolicy) throws Exception {
		if(splashScreen != null){
			splashScreen.setMessage("Resolving root dependency versions...");
			splashScreen.incrementProgress(10);
		}
		resolveModelVersions(models, upgradePolicy);
		if(splashScreen != null){
			splashScreen.clearMessage();
		}
		if (!(upgradePolicy == null || upgradePolicy.equals(UpgradePolicy.never))) {
			if(splashScreen != null){
				splashScreen.setMessage("Upgrading dependencies...");
				splashScreen.incrementProgress(10);
			}
			upgrade(models, upgradePolicy);
			
			if(splashScreen != null){
				splashScreen.clearMessage();
			}
		}
		if(splashScreen != null){
			splashScreen.setMessage("Resolving dependencies...");
			splashScreen.incrementProgress(10);
		}
		resolve(models);
		if(splashScreen != null){
			splashScreen.clearMessage();
		}
		List<ModelNode> rootNodes = new ArrayList<ModelNode>();
		for (ModelInfo modelInfo : models) {
			ModelNode modelNode = modelNodeResult.getModelNode(modelInfo.getId());
			if (modelNode != null) {
				rootNodes.add(modelNode);
				if (modelNode.getArtifact(null) == null) {
					throw new Exception("The artifact for dependency " + modelNode.getId() + " does not exist.");
				}
			} else {
				if (bootstrapperLog.isLoggable(Level.SEVERE)) {
					bootstrapperLog.log(Level.SEVERE, "Cannot find resolved model for the dependency: " + modelInfo.getId() + ". The dependency will be ignored in the classloader construction.");
				}
			}
		}
		BootClassLoader bootClassLoader = null;
		if(splashScreen != null){
			splashScreen.setMessage("Initializing class loaders...");
			splashScreen.incrementProgress(10);
		}
		Set<String> exclusions = new HashSet<String>();
		if(excludes != null) {
		    for(ModelInfo modelInfo : excludes) {
			if((modelInfo.getGroupId() != null) && (modelInfo.getArtifactId() != null)) {
			    exclusions.add(modelInfo.getGroupId() + ":" + modelInfo.getArtifactId());
			}
		    }
		}
		if (parent != null) {
			bootClassLoader = new BootClassLoader(parent, modelNodeResult, rootNodes, this, localResources, localClasses, exclusions);
		} else {
			bootClassLoader = new BootClassLoader(modelNodeResult, rootNodes, this, localResources, localClasses, exclusions);
		}
		if(splashScreen != null){
			splashScreen.clearMessage();
		}
		return bootClassLoader;
	}

	public void pruneModelNodes() {
	    modelNodeResult.pruneModels();
	    //We can also clear modelNodeResult because the NodeClassLoader instances hold direct references to the ModelNode instances.
	    //This way we get rid of the ModelNodes for which no NodeClassLoader was created
	    modelNodeResult.clear(); 
	}

}
