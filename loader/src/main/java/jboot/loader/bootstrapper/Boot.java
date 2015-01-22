package jboot.loader.bootstrapper;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;

import jboot.loader.bootstrapper.config.BootConfig;
import jboot.loader.bootstrapper.config.CustomArtifact;
import jboot.loader.bootstrapper.config.RepositoryInfo;
import jboot.loader.bootstrapper.config.BootConfig.UpgradePolicy;
import jboot.loader.bootstrapper.splash.SplashScreen;
import jboot.repository.client.info.model.ModelInfo;

public final class Boot {
	public static String BOOT_HOME = null;

	private static final Logger log = Logger.getLogger(Boot.class.getName());
	private static final Logger bootstrapperLog = Logger.getLogger("jboot.bootstrapper");
	private static final String BOOT_PRESENCE_PROPERTY = "jboot.loader.boot";
	private static final String BOOT_HOME_PROPERTY = "jboot.home";
	private static final String DEFAULT_BOOT_HOME = ".jboot";
	private static final String DEFAULT_SPLASH_IMAGE = "defaultSplashScreen.png";
	private static final String strARG_CONFIG_URI = "-configfile:";
	private static final String strARG_SPLASH_IMAGE = "-splash:";
	private static final String strARG_SHOW_SPLASH = "-showsplash:";
	private static final String strARG_TARGET = "-target:";
	private static final String strARG_HELP1 = "-h";
	private static final String strARG_HELP2 = "-help";
	private static final String strARG_HELP3 = "/?";
	private static final String strARG_LOCAL_REPO = "-localrepo:";
	private static final String strARG_UPGRADE_POLICY = "-upgradepolicy:";
	private static final String strARG_REMOTE_REPO = "-remoterepo:";
	private static final String strARG_CUSTOM_ARTIFACT = "-customartifact:";
	private static final String strARG_DEPENDENCY = "-dependency:";
	private static final String strARG_EXCLUDE = "-exclude:";
	private static final String strARG_BOOTABLE_URI = "-bootablefile:";
	private static final String strARG_LOG_LEVEL = "-loglevel:";
	private static final String strDEFAULT_CONFIG_URI = System.getProperty("user.dir") + File.separator + "config.xml";
	private static String strDEFAULT_LOCAL_REPO = null;
	private static final String strDEFAULT_UPGRADE_POLICY_ALWAYS = "always";
	private static final String strDEFAULT_UPGRADE_POLICY_NEVER = "never";

	static {
		System.setProperty(BOOT_PRESENCE_PROPERTY, "true");

		BOOT_HOME = System.getProperty(BOOT_HOME_PROPERTY);
		if (BOOT_HOME == null || BOOT_HOME.trim().isEmpty()) {
			System.setProperty(BOOT_HOME_PROPERTY, DEFAULT_BOOT_HOME);
			BOOT_HOME = DEFAULT_BOOT_HOME;
		}

		strDEFAULT_LOCAL_REPO = System.getProperty("user.dir") + File.separator + BOOT_HOME + File.separator + "repository";
	}

	private static Boot thisBoot;

	private String[] rawArgs;
	private boolean bPrintHelp;
	private Level logLevel;
	private String bootConfigLocation;
//	private String bootableName;
//	private String bootableVersion;
	private List<String> programArgs;

	private Unmarshaller unmarshaller;
	private XMLInputFactory xmlInputFactory;
	private XMLOutputFactory xmlOutputFactory;

	private Bootstrapper bootstrapper;
	private BootConfig fileBootConfig;
	private BootConfig cliBootConfig;
	private BootConfig effectiveBootConfig;
	
    private static SplashScreen splashScreen;

	public static void main(String[] args) {
		try {
			thisBoot = new Boot(args);
			thisBoot.boot();
		} catch (Exception ex) {
			if (bootstrapperLog.isLoggable(Level.SEVERE)) {
				bootstrapperLog.log(Level.SEVERE, "Exception caught while bootstrapping.", ex);
			}
			throw new RuntimeException(ex);
		}
	}

	public Boot(String[] args) throws Exception {
		rawArgs = args;
		JAXBContext ctx = JAXBContext.newInstance(BootConfig.class);
		unmarshaller = ctx.createUnmarshaller();
		xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
		xmlOutputFactory = XMLOutputFactory.newInstance();
	}

	public void boot() throws Exception {
		parseArguments();
		configureLogging();
		loadBootConfig();
		createEffectiveBootConfig();
		
		if (effectiveBootConfig.isShowSplash() != null && effectiveBootConfig.isShowSplash()) {
            if (effectiveBootConfig.getSplashImage() != null && !effectiveBootConfig.getSplashImage().isEmpty()) {
                splashScreen = new SplashScreen(effectiveBootConfig.getSplashImage());
            } else {
                splashScreen = new SplashScreen(DEFAULT_SPLASH_IMAGE);
            }
            splashScreen.showSplashScreen();
        }
		try {
			Bootstrapper bootstrapper = null;
			try {
				bootstrapper = getBootstrapper();
			} catch (BootConfigurationException ex) {
				printHelp();
				printBootConfig(effectiveBootConfig);
				throw ex;
			}
	
			if (bPrintHelp == true) {
				printHelp();
				printBootConfig(effectiveBootConfig);
				bootstrapper.printBootablesHelp();
				if (Level.FINEST.equals(logLevel)) {
					System.out.println("System properties:");
					System.getProperties().list(new PrintStream(System.out));
				}
				return;
			}
	
			if (effectiveBootConfig.getTarget() != null &&
				effectiveBootConfig.getTarget().getName() != null &&
				!effectiveBootConfig.getTarget().getName().trim().isEmpty()) {
				bootstrapper.bootstrap(programArgs.toArray(new String[0]));
			} else {
				printHelp();
				printBootConfig(effectiveBootConfig);
				bootstrapper.printBootablesHelp();
			}
		} finally {
			if(splashScreen != null){
				splashScreen.removeSplashScreen();
				splashScreen = null;
			} 
		}
	}

	private void configureLogging() throws IOException {
		if (System.getProperty("java.util.logging.config.file") == null) {
			InputStream is = Boot.class.getClassLoader().getResourceAsStream("jboot/loader/bootstrapper/log/logging.properties");
			if (is != null) {
				LogManager.getLogManager().readConfiguration(is);
			}
			if (logLevel != null) {
				Logger jbootRootLogger = Logger.getLogger("jboot");
				jbootRootLogger.setLevel(logLevel);
				Logger rootLogger = LogManager.getLogManager().getLogger("");
				for (Handler handler : rootLogger.getHandlers()) {
					handler.setLevel(logLevel);
				}
			}
		}
	}

	private Bootstrapper getBootstrapper() throws Exception {
		if (bootstrapper == null) {
			if(splashScreen != null){
				splashScreen.setMessage("Validating boot configuration...");
				splashScreen.setMaximumProgress(90);
				splashScreen.incrementProgress(10);
			}
			validateBootConfig(effectiveBootConfig);
			if(splashScreen != null){
				splashScreen.clearMessage();
			}
			if(splashScreen != null){
				splashScreen.setMessage("Initializing boot strapper...");
				splashScreen.incrementProgress(10);
			}
			bootstrapper = new Bootstrapper(effectiveBootConfig, splashScreen);
			String[] bootConfigArgs = generateCliArguments(effectiveBootConfig);
			if (bootConfigArgs.length > 0) {
				bootstrapper.setBootConfigArgs(bootConfigArgs);
			}
		}
		return bootstrapper;
	}

	private void printHelp() {
		System.out.println("Boot command line syntax: Boot [arguments]");

		System.out.println("\t" + strARG_CONFIG_URI + "<uri>");
		System.out.println("\t\tSpecifies the location of the boot configuration file.\n\t\tDefault value: ./config.xml\n");

		System.out.println("\t" + strARG_TARGET + "<name>[:<version>]");
		System.out.println("\t\tSpecifies the name and optionally the version of the bootable target to launch.\n");

		System.out.println("\t" + strARG_LOCAL_REPO + "<path>");
		System.out.println("\t\tSpecifies the path to the local artifacts repository. This argument can be repeated to specify multiple local repositories.\n\t\tDefault value: ./" + BOOT_HOME + "/repository\n");

		System.out.println("\t" + strARG_UPGRADE_POLICY + "[always|never|tryandcontinue]");
		System.out.println("\t\tSpecifies the policy to use for upgrading artifacts in the local repository.\n" +
						   "\t\tDefault value: '" + strDEFAULT_UPGRADE_POLICY_ALWAYS + "' when at least one remote repository and exactly one local repository are specified, otherwise '" + strDEFAULT_UPGRADE_POLICY_NEVER + "'\n");

		System.out.println("\t" + strARG_REMOTE_REPO + "<id>:<name>:<host>:<port>:<version>");
		System.out.println("\t\tSpecifies a remote repository. This argument can be repeated to specify multiple remote repositories.\n");

		System.out.println("\t" + strARG_CUSTOM_ARTIFACT + "<groupid>::<artifactid>::<version>::[<classifier>]::[<pom>][::<resource>...]");
		System.out.println("\t\tSpecifies a custom artifact. This argument can be repeated to specify multiple custom artifacts.\n");

		System.out.println("\t" + strARG_DEPENDENCY + "<groupid>:<artifactid>:<version>");
		System.out.println("\t\tSpecifies a dependency. This argument can be repeated to specify multiple dependencies.\n");
		
		System.out.println("\t" + strARG_EXCLUDE + "<groupid>:<artifactid>");
		System.out.println("\t\tSpecifies a global exclude. This argument can be repeated to specify multiple global excludes.\n");

		System.out.println("\t" + strARG_BOOTABLE_URI + "<uri>");
		System.out.println("\t\tSpecifies the location of a file containing bootable target definitions. This argument can be repeated to specify multiple file locations.\n");

		System.out.println("\t" + strARG_HELP1 + " | " + strARG_HELP2 + " | " + strARG_HELP3);
		System.out.println("\t\tDisplays this message and lists the discovered bootable targets and their description.\n");

		System.out.println("\t" + strARG_LOG_LEVEL + "[INFO|FINE|FINER|FINEST]");
		System.out.println("\t\tSpecifies the granularity of log messages.\n\t\tDefault value: INFO\n");
		
		System.out.println("\t" + strARG_SHOW_SPLASH + "[true|false]");
        System.out.println("\t\tDetermines if the splash screen is displayed.\n\t\tDefault value: false\n");
        
        System.out.println("\t" + strARG_SPLASH_IMAGE + "<path>");
        System.out.println("\t\tSpecifies the path of the splash image.\n\t\tDefault value: defaultSplashScreen.png\n");
	}

	private void printBootConfig(BootConfig bootconfig) {
		System.out.println("------------------");
		System.out.println("Boot Configuration");
		System.out.println("------------------");
		System.out.println();
		if (bootconfig.getLocalRepositories().size() > 0) {
			System.out.println("Local repositories:");
			for (String localRepoPath : bootconfig.getLocalRepositories()) {
				System.out.println(localRepoPath);
			}
		} else {
			System.out.println("No local repositories specified.");			
		}
		System.out.println();
		if (bootconfig.getRepositories().size() > 0) {
			System.out.println("Remote repositories:");
			for (RepositoryInfo repositoryInfo : bootconfig.getRepositories()) {
				System.out.println("[id: " + repositoryInfo.getId() + "][name: " + repositoryInfo.getName() + "][url: " + repositoryInfo.getUrl() + "][version: " + repositoryInfo.getVersion() + "]");
			}
		} else {
			System.out.println("No remote repositories specified.");
		}
		System.out.println();
		if (bootconfig.getCustomArtifacts().size() > 0) {
			System.out.println("Custom artifacts:");
			for (CustomArtifact customArtifact : bootconfig.getCustomArtifacts()) {
				System.out.println(customArtifact.getGroupId() + ":" + customArtifact.getArtifactId() + ":" + customArtifact.getVersion() + ":" + customArtifact.getClassifier());
				System.out.println("\t" + ((customArtifact.getPom() != null && !customArtifact.getPom().trim().isEmpty()) ? "pom: " + customArtifact.getPom() : "No custom artifact pom specified."));
				if (customArtifact.getResources().size() > 0) {
					for (String resourcePath : customArtifact.getResources()) {
						System.out.println("\tresource: " + resourcePath);
					}
				} else {
					System.out.println("\tNo custom artifact resources specified.");
				}
			}
		} else {
			System.out.println("No custom artifacts specified.");
		}
		System.out.println();
		System.out.println("Upgrade policy: " + bootconfig.getUpgradePolicy());
		System.out.println();
		if (bootconfig.getDependencies().size() > 0) {
			System.out.println("Dependencies:");
			for (ModelInfo modelInfo : bootconfig.getDependencies()) {
				System.out.println(modelInfo.getId());
			}
		} else {
			System.out.println("No dependencies specified.");			
		}
		System.out.println();
		if (bootconfig.getExcludes().size() > 0) {
			System.out.println("Excludes:");
			for (ModelInfo modelInfo : bootconfig.getExcludes()) {
				System.out.println(modelInfo.getId());
			}
		} else {
			System.out.println("No excludes specified.");			
		}
		System.out.println();
		if (bootconfig.getArguments().size() > 0) {
			System.out.println("Default arguments:");
			for (String arg : bootconfig.getArguments()) {
				System.out.println(arg);
			}
		} else {
			System.out.println("No default arguments specified.");
		}
		System.out.println();
		if (bootconfig.getBootableUris().size() > 0) {
			System.out.println("Bootables URIs:");
			for (String bootableUri : bootconfig.getBootableUris()) {
				System.out.println(bootableUri);
			}
		} else {
			System.out.println("No bootable targets definition files specified.");
		}
		if (bootconfig.getTarget() != null) {
			System.out.println();
			System.out.println("Bootable target: " + bootconfig.getTarget().getName() + ":" + bootconfig.getTarget().getVersion());
		}
		if (programArgs != null && programArgs.size() > 0) {
			System.out.println();
			System.out.println("CLI arguments:");
			for (String arg : programArgs) {
				System.out.println(arg);
			}
		}
		System.out.println();
	}

	private void createEffectiveBootConfig() {
		if (fileBootConfig != null) {
			effectiveBootConfig = fileBootConfig;

			for (String localRepoPath : cliBootConfig.getLocalRepositories()) {
				if (localRepoPath != null && !localRepoPath.trim().isEmpty() && !effectiveBootConfig.getLocalRepositories().contains(localRepoPath)) {
					effectiveBootConfig.getLocalRepositories().add(localRepoPath);
				}
			}

			if (cliBootConfig.getUpgradePolicy() != null) {
				effectiveBootConfig.setUpgradePolicy(cliBootConfig.getUpgradePolicy());
			}

			Map<String, RepositoryInfo> effectiveRepositoriesMap = new LinkedHashMap<String, RepositoryInfo>();
			for (RepositoryInfo cliRepositoryInfo : cliBootConfig.getRepositories()) {
				if (cliRepositoryInfo.getId() != null) {
					effectiveRepositoriesMap.put(cliRepositoryInfo.getId(), cliRepositoryInfo);
				}
			}
			for (RepositoryInfo effRepositoryInfo : effectiveBootConfig.getRepositories()) {
				if (!effectiveRepositoriesMap.containsKey(effRepositoryInfo.getId())) { //do not override the CLI specified repos
					effectiveRepositoriesMap.put(effRepositoryInfo.getId(), effRepositoryInfo);
				}
			}
			effectiveBootConfig.getRepositories().clear();
			effectiveBootConfig.getRepositories().addAll(effectiveRepositoriesMap.values());

			for (CustomArtifact cliCustomArtifact : cliBootConfig.getCustomArtifacts()) {
				Iterator<CustomArtifact> iter = effectiveBootConfig.getCustomArtifacts().iterator();
				while (iter.hasNext()) {
					CustomArtifact effCustomArtifact = iter.next();
					if (effCustomArtifact.equals(cliCustomArtifact)) {
						iter.remove();
					}
				}
				effectiveBootConfig.getCustomArtifacts().add(cliCustomArtifact);
			}

			for (ModelInfo cliModelInfo : cliBootConfig.getDependencies()) {
				Iterator<ModelInfo> iter = effectiveBootConfig.getDependencies().iterator();
				while (iter.hasNext()) {
					ModelInfo effModelInfo = iter.next();
					if (cliModelInfo.getGroupId() == null || cliModelInfo.getArtifactId() == null ||
						effModelInfo.getGroupId() == null || effModelInfo.getArtifactId() == null) {
						continue;
					}
					if (cliModelInfo.getGroupId().equals(effModelInfo.getGroupId()) &&
						cliModelInfo.getArtifactId().equals(effModelInfo.getArtifactId())) {
						iter.remove();
					}
				}
				effectiveBootConfig.getDependencies().add(cliModelInfo);
			}
			
			for (ModelInfo cliModelInfo : cliBootConfig.getExcludes()) {
				Iterator<ModelInfo> iter = effectiveBootConfig.getExcludes().iterator();
				while (iter.hasNext()) {
					ModelInfo effModelInfo = iter.next();
					if (cliModelInfo.getGroupId() == null || cliModelInfo.getArtifactId() == null ||
						effModelInfo.getGroupId() == null || effModelInfo.getArtifactId() == null) {
						continue;
					}
					if (cliModelInfo.getGroupId().equals(effModelInfo.getGroupId()) &&
						cliModelInfo.getArtifactId().equals(effModelInfo.getArtifactId())) {
						iter.remove();
					}
				}
				effectiveBootConfig.getExcludes().add(cliModelInfo);
			}

			//cliBootConfig arguments are empty. the cli arguments passed to the bootstrapper are in programArgs.

			for (String bootableUri : cliBootConfig.getBootableUris()) {
				effectiveBootConfig.getBootableUris().add(bootableUri);
			}

			if (cliBootConfig.getTarget() != null) {
				effectiveBootConfig.setTarget(cliBootConfig.getTarget());
			}
			
			if(cliBootConfig.isShowSplash() != null) {
				effectiveBootConfig.setShowSplash(cliBootConfig.isShowSplash());
			}
			
			if(cliBootConfig.getSplashImage() != null) {
				effectiveBootConfig.setSplashImage(cliBootConfig.getSplashImage());
			}
		} else {
			effectiveBootConfig = cliBootConfig;
		}

		//set defaults
		if (effectiveBootConfig.getLocalRepositories().size() == 0) {
			effectiveBootConfig.getLocalRepositories().add(strDEFAULT_LOCAL_REPO);
		}
		if (effectiveBootConfig.getUpgradePolicy() == null) {
			if (effectiveBootConfig.getRepositories().size() > 0 && effectiveBootConfig.getLocalRepositories().size() == 1) {
				effectiveBootConfig.setUpgradePolicy(UpgradePolicy.valueOf(strDEFAULT_UPGRADE_POLICY_ALWAYS));
			} else {
				effectiveBootConfig.setUpgradePolicy(UpgradePolicy.valueOf(strDEFAULT_UPGRADE_POLICY_NEVER));
			}
		}
	}

	private void validateBootConfig(BootConfig bootconfig) throws BootConfigurationException {
		if (bootconfig.getLocalRepositories().size() == 0) {
			throw new BootConfigurationException("No Local repository path is specified.");
		}
		for (String strLocalRepoPath : bootconfig.getLocalRepositories()) {
			File localRepoDir = new File(strLocalRepoPath);
			if (localRepoDir.exists()) {
				if (!localRepoDir.isDirectory()) {
					throw new BootConfigurationException("Local repository path " + strLocalRepoPath + " is not a directory.");
				}
			} else {
				localRepoDir.mkdirs();
			}
		}
		if (bootconfig.getUpgradePolicy() == UpgradePolicy.always && bootconfig.getRepositories().size() == 0) {
			throw new BootConfigurationException("Remote repository URL(s) not specified. At least one remote repository URL should be specified.");
		}
		if (bootconfig.getDependencies().size() == 0) {
			throw new BootConfigurationException("Dependency(ies) not specified. At least one dependency should be specified.");
		}
		for (CustomArtifact customArtifact : bootconfig.getCustomArtifacts()) {
			if (customArtifact.getGroupId() == null || customArtifact.getGroupId().trim().isEmpty()) {
				throw new BootConfigurationException("A custom artifact specification has a missing groupId.");
			}
			if (customArtifact.getArtifactId() == null || customArtifact.getArtifactId().trim().isEmpty()) {
				throw new BootConfigurationException("A custom artifact specification has a missing artifactId.");
			}
			if (customArtifact.getVersion() == null || customArtifact.getVersion().trim().isEmpty()) {
				throw new BootConfigurationException("A custom artifact specification has a missing version.");
			}
		}
	}

	private void parseArguments() {
		programArgs = new ArrayList<String>(rawArgs.length);
		CliBootConfigBuilder cliBootConfigBuilder = new CliBootConfigBuilder();
		for (String arg : rawArgs) {
			if (arg != null) {
				if (arg.startsWith(strARG_CONFIG_URI)) {
					bootConfigLocation = arg.substring(strARG_CONFIG_URI.length());
				} else if (arg.equals(strARG_HELP1) || arg.equals(strARG_HELP2) || arg.equals(strARG_HELP3)) {
					bPrintHelp = true;
				} else if (arg.startsWith(strARG_LOG_LEVEL)) {
					try {
						logLevel = Level.parse(arg.substring(strARG_LOG_LEVEL.length()));
					} catch (IllegalArgumentException ex) {
						ex.printStackTrace();
					}
				} else if (arg.startsWith(strARG_SHOW_SPLASH)) {
					if(arg.substring(strARG_SHOW_SPLASH.length()).equals("true")){
						cliBootConfigBuilder.setShowSplash(true);
					} else {
						cliBootConfigBuilder.setShowSplash(false);
					}
				} else if (arg.startsWith(strARG_SPLASH_IMAGE)) {
					cliBootConfigBuilder.setSplashImage(arg.substring(strARG_SPLASH_IMAGE.length()));
				} else if (arg.startsWith(strARG_LOCAL_REPO)) {
					cliBootConfigBuilder.addLocalReposisotry(arg.substring(strARG_LOCAL_REPO.length()));
				} else if (arg.startsWith(strARG_UPGRADE_POLICY)) {
					cliBootConfigBuilder.setUpgradePolicy(arg.substring(strARG_UPGRADE_POLICY.length()));
				} else if (arg.startsWith(strARG_REMOTE_REPO)) {
					cliBootConfigBuilder.addRemoteRepository(arg.substring(strARG_REMOTE_REPO.length()));
				} else if (arg.startsWith(strARG_CUSTOM_ARTIFACT)) {
					cliBootConfigBuilder.addCustomArtifact(arg.substring(strARG_CUSTOM_ARTIFACT.length()));
				} else if (arg.startsWith(strARG_DEPENDENCY)) {
					cliBootConfigBuilder.addDependency(arg.substring(strARG_DEPENDENCY.length()));
				} else if (arg.startsWith(strARG_EXCLUDE)) {
					cliBootConfigBuilder.addExclude(arg.substring(strARG_EXCLUDE.length())); 
				} else if (arg.startsWith(strARG_BOOTABLE_URI)) {
					cliBootConfigBuilder.addBootableUri(arg.substring(strARG_BOOTABLE_URI.length()));
				} else if (arg.startsWith(strARG_TARGET)) {
					cliBootConfigBuilder.setTarget(arg.substring(strARG_TARGET.length()));
				} else {
					programArgs.add(arg);
				}
			}
		}
		cliBootConfig = cliBootConfigBuilder.createBootConfig();
	}

	private void loadBootConfig() throws Exception {
		InputStream in = null;
		if (bootConfigLocation != null) { //passed from command-line.
			in = findBootConfig(bootConfigLocation);
			if (in == null) {
				throw new Exception("Cannot find boot configuration: " + bootConfigLocation);
			}
		} else { //try to use default boot config location.
			in = findBootConfig(strDEFAULT_CONFIG_URI);
		}
		if (in != null) {
			InputStream bootConfigInputStream = extractBootConfigFromStream(in);
			fileBootConfig = (BootConfig) unmarshaller.unmarshal(bootConfigInputStream);
		}
	}

	private InputStream findBootConfig(String bootConfigLocation) {
		InputStream configStream = null;
		//try to get the config as a file
		File configfile = new File(bootConfigLocation);
		if (configfile.exists() && configfile.isFile()) {
			try {
				configStream = new FileInputStream(configfile);
			} catch (Exception ex) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Could not create a FileInputStream to the boot configuration file at: " + bootConfigLocation, ex);
				}
			}
		}
		if (configStream == null) {
			//try to get the config as a resource
			configStream = this.getClass().getResourceAsStream(bootConfigLocation);
		}
		if (configStream == null) {
			//try to get the config from url
			try {
				URL configUrl = new URI(bootConfigLocation).toURL();
				URLConnection urlConnection = configUrl.openConnection();
				configStream = urlConnection.getInputStream();
			} catch (Exception ex) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Could not open a URLConnection to the boot configuration file at: " + bootConfigLocation, ex);
				}
			}
		}
		return configStream;
	}

	private InputStream extractBootConfigFromStream(InputStream in) throws Exception {
		boolean isInBoot = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLEventWriter xmlEventWriter = null;
		XMLEventReader xmlEventReader = null;
		try {
			xmlEventWriter = xmlOutputFactory.createXMLEventWriter(baos);
			xmlEventReader = xmlInputFactory.createXMLEventReader(new BufferedInputStream(in));
			while (xmlEventReader.hasNext()) {
				XMLEvent event = xmlEventReader.nextEvent();
				if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("boot")) {
					isInBoot = true;
				}
				if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("boot")) {
					isInBoot = false;
					xmlEventWriter.add(event);
				}
				if (isInBoot) {
					xmlEventWriter.add(event);
				}
			}
		} finally {
			if (xmlEventReader != null) {
				xmlEventReader.close();
			}
			if (in != null) {
				in.close();
			}
			if (xmlEventWriter != null) {
				xmlEventWriter.flush();
				xmlEventWriter.close();
			}
		}
		return new ByteArrayInputStream(baos.toByteArray());
	}

	private String[] generateCliArguments(BootConfig bootConfig) {
		List<String> args = new ArrayList<String>();
		for (String localRepo : bootConfig.getLocalRepositories()) {
			args.add(strARG_LOCAL_REPO + localRepo);
		}
		if (bootConfig.getUpgradePolicy() != null) {
			args.add(strARG_UPGRADE_POLICY + bootConfig.getUpgradePolicy());
		}
		for (RepositoryInfo repoInfo : bootConfig.getRepositories()) {
			String strHost = repoInfo.getUrl().replaceAll("http://", "").replaceAll("/", "");
			String strPort = "";
			if (strHost.lastIndexOf(':') > 0) {
				strPort = strHost.substring(strHost.lastIndexOf(':')+1);
			}
			args.add(strARG_REMOTE_REPO + repoInfo.getId() + ":" + repoInfo.getName() + ":" + strHost + ":" + strPort + ":" + repoInfo.getVersion());
		}
		for (ModelInfo dependency : bootConfig.getDependencies()) {
			args.add(strARG_DEPENDENCY + dependency.getId());
		}
		for (ModelInfo exclude : bootConfig.getExcludes()) {
			args.add(strARG_EXCLUDE + exclude.getId());
		}
		for (CustomArtifact customArtifact : bootConfig.getCustomArtifacts()) {
			String strCustArt = strARG_CUSTOM_ARTIFACT + customArtifact.getGroupId() + "::" + customArtifact.getArtifactId() + "::" + customArtifact.getVersion() + "::" + ((customArtifact.getClassifier()!=null)?customArtifact.getClassifier():"") + "::" + ((customArtifact.getPom()!=null)?customArtifact.getPom():"");
			for (String res : customArtifact.getResources()) {
				strCustArt += "::" + res;
			}
		}
		for (String uri : bootConfig.getBootableUris()) {
			args.add(strARG_BOOTABLE_URI + uri);
		}
		if (bootConfig.getTarget() != null) {
			String strTarget = bootConfig.getTarget().getName() != null ? bootConfig.getTarget().getName() : "";
			if (bootConfig.getTarget().getVersion() != null && !bootConfig.getTarget().getVersion().trim().isEmpty()) {
				strTarget += ":" + bootConfig.getTarget().getVersion();
			}
			args.add(strARG_TARGET + strTarget);
		}
		return args.toArray(new String[0]);
	}

	public static class BootConfigurationException extends Exception {
		private static final long serialVersionUID = 6481063775958830057L;

		public BootConfigurationException(String message) {
			super(message);
		}

		public BootConfigurationException(String message, Throwable cause) {
			super(message, cause);
		}

		public BootConfigurationException(Throwable cause) {
			super(cause);
		}
	}
}
