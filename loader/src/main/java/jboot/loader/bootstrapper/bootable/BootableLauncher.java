package jboot.loader.bootstrapper.bootable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import jboot.loader.bootstrapper.bootable.model.ArgumentInfo;
import jboot.loader.bootstrapper.bootable.model.BootableInfo;
import jboot.loader.bootstrapper.bootable.model.BootableInfoRef;
import jboot.loader.bootstrapper.bootable.model.BootableInfos;
import jboot.loader.bootstrapper.bootable.model.JavaBootableInfo;
import jboot.loader.bootstrapper.bootable.model.ScriptBootableInfo;

public class BootableLauncher extends AbstractBootableLauncher<BootableInfo> {
	private static final Logger bootstrapperLog = Logger.getLogger("jboot.bootstrapper");
	private static final Logger log = Logger.getLogger(BootableLauncher.class.getName());
	private static final String strBOOTABLES_FILE_PATH = "jboot/boot/targets.xml";

	private Map<String, BootableInfo> bootables; //map key == name:version
	private List<String> bootableUris;
	private Unmarshaller unmarshaller;
	private Marshaller marshaller;
	private JavaBootableLauncher javaBootableLauncher;
	private ScriptBootableLauncher scriptBootableLauncher;
	private Set<BootableInfo> inheritedBootables;

	public BootableLauncher(ClassLoader classLoader) throws Exception {
		super(classLoader);
		JAXBContext ctx = JAXBContext.newInstance(BootableInfos.class, BootableInfo.class, JavaBootableInfo.class, ScriptBootableInfo.class);
		unmarshaller = ctx.createUnmarshaller();
		marshaller = ctx.createMarshaller();
		inheritedBootables = new LinkedHashSet<BootableInfo>();
		bootables = loadBootablesFromClassloader();
	}

	public BootableLauncher(ClassLoader classLoader, List<String> bootableUris) throws Exception {
		this(classLoader);
		this.bootableUris = bootableUris;
		bootables.putAll(loadBootablesFromUris());
	}

	public BootableInfo getBootable(String bootableName, String bootableVersion) {
		return bootables.get(bootableName + ":" + bootableVersion);
	}

	public BootableInfo getLatestBootable(String bootableName) {
		String bootableVersion = findLatestBootableVersion(bootableName);
		if (bootableVersion != null) {
			return bootables.get(bootableName + ":" + bootableVersion);
		}
		return null;
	}

	public List<String> getBootableUris() {
		return bootableUris;
	}

	private BootableInfo resolveParents(BootableInfo bootable, Set<BootableInfo> resolvedParents) throws Exception {
		if (bootable.getParent() != null) {
			return bootable;
		}
		if (bootable.getParentRef() != null) {
			if (bootable.getParentRef().getName() == null || bootable.getParentRef().getName().trim().isEmpty()) {
				throw new Exception("The bootable target " + bootable.getName() + ":" + bootable.getVersion() + " references a parent bootable target with an unspecified name.");
			}
			if (bootable.getParentRef().getVersion() == null || bootable.getParentRef().getVersion().trim().isEmpty()) {
				String version = findLatestBootableVersion(bootable.getParentRef().getName());
				if (version == null) {
					throw new Exception("The bootable target " + bootable.getName() + ":" + bootable.getVersion() + " references a parent bootable target " + bootable.getParentRef().getName() + " that does not exist.");
				}
				bootable.getParentRef().setVersion(version);
			}
			BootableInfo parentBootable = getBootable(bootable.getParentRef().getName(), bootable.getParentRef().getVersion());
			if (parentBootable == null) {
				throw new Exception("The bootable target " + bootable.getName() + ":" + bootable.getVersion() + " references a parent bootable target " + bootable.getParentRef().getName() + ":" + bootable.getParentRef().getVersion() + " that doesn't exist.");				
			}
			if (!bootable.getClass().isAssignableFrom(parentBootable.getClass())) {
				throw new Exception("The bootable target " + bootable.getName() + ":" + bootable.getVersion() + " references a parent bootable target " + bootable.getParentRef().getName() + ":" + bootable.getParentRef().getVersion() + " that doesn't have the same type. expected type: " + bootable.getClass().getSimpleName() + " actual type: " + parentBootable.getClass().getSimpleName());				
			}
			if (resolvedParents.contains(parentBootable)) {
				throw new Exception("Cycle detected in the bootable targets parent-child hierarchy. The bootable target " + parentBootable.getName() + ":" + parentBootable.getVersion() + " is both a parent and a descendant of the bootable target " + bootable.getName() + ":" + bootable.getVersion());
			}
			resolvedParents.add(parentBootable);
			bootable.setParent(parentBootable);
			resolveParents(parentBootable, resolvedParents);
		}
		return bootable;
	}

	public void printHelp() throws Exception {
		System.out.println("-----------------");
		System.out.println("Bootable targets:");
		System.out.println("-----------------");
		if (bootables.size() == 0) {
			System.out.println();
			System.out.println("No bootable targets discovered.");
		}
		for (BootableInfo bootable : bootables.values()) {
			if (!bootable.isAbstractBootable()) {
				System.out.println();
				bootable = inheritParents(bootable);
				printBootable(bootable);
			}
		}
		System.out.println();
	}

	public void printBootable(BootableInfo bootable) throws Exception {
		System.out.println("Target: " + bootable.getName() + ":" + bootable.getVersion());
		if (bootable.getParentRef() != null) {
			System.out.println("\tParent: " + bootable.getParentRef().getName() + ":" + ((bootable.getParentRef().getVersion() != null) ? bootable.getParentRef().getVersion() : "null"));
		}
		System.out.println("\tDescription: " + bootable.getDescription());
		if (bootable.getArgumentInfos().size() > 0) {
			System.out.println("\tArguments info:");
			for (ArgumentInfo argumentInfo : bootable.getArgumentInfos()) {
				System.out.println("\t\t" + argumentInfo.getArgument() + "\n\t\t\t" + argumentInfo.getDescription());
			}
		}
		if (bootable.getArguments().size() > 0) {
			System.out.println("\tPredefined arguments:");
			for (String argument : bootable.getArguments()) {
				System.out.println("\t\t" + argument);
			}
		}
		if (bootable.getPreBootables().size() > 0) {
			System.out.println("\tPre bootable targets:");
			for (BootableInfoRef bootableRef : bootable.getPreBootables()) {
				if (bootableRef != null) {
					System.out.println("\t\t" + bootableRef.getName() + ":" + ((bootableRef.getVersion() != null) ? bootableRef.getVersion() : "null"));
				}
			}
		}
		if (bootable.getPostBootables().size() > 0) {
			System.out.println("\tPost bootable targets:");
			for (BootableInfoRef bootableRef : bootable.getPostBootables()) {
				if (bootableRef != null) {
					System.out.println("\t\t" + bootableRef.getName() + ":" + ((bootableRef.getVersion() != null) ? bootableRef.getVersion() : "null"));
				}
			}
		}
	}

	private void dumpBootable(BootableInfo bootable, File f) throws Exception {
		marshaller.marshal(bootable, f);
	}

	private BootableInfo inheritParentsInternal(BootableInfo bootable, Set<BootableInfo> inherited) throws Exception {
		if (!inherited.contains(bootable)) {
			inherited.add(bootable);
			BootableInfo parentBootable = bootable.getParent();
			if (parentBootable != null) {
				parentBootable = inheritParentsInternal(parentBootable, inherited);
				bootable.getArgumentInfos().addAll(0, parentBootable.getArgumentInfos());
				bootable.getArguments().addAll(0, parentBootable.getArguments());
				bootable.getPreBootables().addAll(0, parentBootable.getPreBootables());
				bootable.getPostBootables().addAll(0, parentBootable.getPostBootables());
			}
		}
		return bootable;
	}

	@Override
	protected BootableInfo inheritParents(BootableInfo bootable, Set<BootableInfo> inherited) throws Exception {
		Set<BootableInfo> inheritedCopy = new LinkedHashSet<BootableInfo>(inherited);
		bootable = inheritParentsInternal(bootable, inheritedCopy);
		if (bootable instanceof JavaBootableInfo) {
			bootable = getJavaBootableLauncher().inheritParents((JavaBootableInfo) bootable, new LinkedHashSet<BootableInfo>(inherited));
		} else if (bootable instanceof ScriptBootableInfo) {
			bootable = getScriptBootableLauncher().inheritParents((ScriptBootableInfo) bootable, new LinkedHashSet<BootableInfo>(inherited));
		} else {
			throw new Exception("The specified bootable target " + bootable.getName() + ":" + bootable.getVersion() + " is of an unknown type.");
		}
		inherited.addAll(inheritedCopy);
		return bootable;
	}

	public BootableInfo inheritParents(BootableInfo bootable) throws Exception {
		bootable = resolveParents(bootable, new LinkedHashSet<BootableInfo>());
		bootable = inheritParents(bootable, inheritedBootables);
		return bootable;
	}

	@Override
	public void launch(BootableInfo bootable, String[] args) throws Exception {
		if (log.isLoggable(Level.FINER)) {
			File f = new File("./effective_bootable_target.xml");
			log.log(Level.FINER, "Dumping effective bootable target at " + f.getAbsolutePath());
			dumpBootable(bootable, f);
		}
		//process pre-bootables
		for (BootableInfoRef bootableInfoRef : bootable.getPreBootables()) {
			String bootableName = bootableInfoRef.getName();
			String bootableVersion = bootableInfoRef.getVersion();
			if (bootableVersion == null || bootableVersion.trim().isEmpty()) {
				bootableVersion = findLatestBootableVersion(bootableInfoRef.getName());
			}
			BootableInfo preBootable = bootables.get(bootableName + ":" + bootableVersion);
			if (preBootable == null) {
				throw new NullPointerException("The specified pre-bootable target " + bootableName + ":" + bootableVersion + " does not exist. Referenced from target: " + bootable.getName() + ":" + bootable.getVersion());
			}
			launch(preBootable, args);
		}

		//launch bootable
		if (bootstrapperLog.isLoggable(Level.INFO)) {
			bootstrapperLog.info("Launching bootable " + bootable.getName() + ":" + bootable.getVersion());
		}
		if (bootable instanceof JavaBootableInfo) {
			getJavaBootableLauncher().launch((JavaBootableInfo) bootable, args);
		} else if (bootable instanceof ScriptBootableInfo) {
			getScriptBootableLauncher().launch((ScriptBootableInfo) bootable, args);
		}  else {
			throw new Exception("The specified bootable target " + bootable.getName() + ":" + bootable.getVersion() + " is of an unknown type.");
		}

		//process post-bootables
		for (BootableInfoRef bootableInfoRef : bootable.getPostBootables()) {
			String bootableName = bootableInfoRef.getName();
			String bootableVersion = bootableInfoRef.getVersion();
			if (bootableVersion == null || bootableVersion.trim().isEmpty()) {
				bootableVersion = findLatestBootableVersion(bootableInfoRef.getName());
			}
			BootableInfo postBootable = bootables.get(bootableName + ":" + bootableVersion);
			if (postBootable == null) {
				throw new NullPointerException("The specified post-bootable target " + bootableName + ":" + bootableVersion + " does not exist. Referenced from target: " + bootable.getName() + ":" + bootable.getVersion());
			}
			launch(postBootable, args);
		}
	}

	private Map<String, BootableInfo> loadBootablesFromClassloader() throws Exception { //map key == name:version
		Map<String, BootableInfo> bootables = new HashMap<String, BootableInfo>();
		Enumeration<URL> resourceUrls = getClassLoader().getResources(strBOOTABLES_FILE_PATH);
		while (resourceUrls.hasMoreElements()) {
			URL url = resourceUrls.nextElement();
			URLConnection urlConnection = url.openConnection();
			try {
				unmarshalBootablesFromStream(bootables, new BufferedInputStream(urlConnection.getInputStream()));
			} catch (Exception ex) {
				throw new Exception("Error caught while reading the bootables at: " + url.toString(), ex);
			}
		}
		return bootables;
	}

	private Map<String, BootableInfo> loadBootablesFromUris() throws Exception { //map key == name:version
		Map<String, BootableInfo> bootables = new HashMap<String, BootableInfo>();
		if (bootableUris != null) {
			for (String strUri : bootableUris) {
				URI uri = URI.create(strUri);
				InputStream is = getInputStreamFromUri(uri);
				if (is != null) {
					try {
						unmarshalBootablesFromStream(bootables, new BufferedInputStream(is));
					} catch (Exception ex) {
						throw new Exception("Error caught while reading the bootables at: " + uri.toString(), ex);
					}
				}
			}
		}
		return bootables;
	}

	private void unmarshalBootablesFromStream(Map<String, BootableInfo> bootables, InputStream is) throws Exception {
		try {
			Object objBootableInfos = unmarshaller.unmarshal(is);
			BootableInfos bootableInfos = (BootableInfos) objBootableInfos;
			for (BootableInfo bootable : bootableInfos.getBootableInfos()) {
				BootableInfo existingBootable = bootables.put(bootable.getName() + ":" + bootable.getVersion(), bootable);
				if (existingBootable != null && bootstrapperLog.isLoggable(Level.WARNING)) {
					bootstrapperLog.warning("Found duplicate bootable definition of " + bootable.getName() + ":" + bootable.getVersion());
				}
			}
		} finally {
			is.close();
		}
	}

	private InputStream getInputStreamFromUri(URI uri) {
		InputStream configStream = null;
		//try to get the uri as a file
		File file = new File(uri.toString());
		if (file.exists() && file.isFile()) {
			try {
				configStream = new FileInputStream(file);
			} catch (Exception ex) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Could not create a FileInputStream to the file at: " + uri, ex);
				}
			}
		}
		if (configStream == null) {
			//try to get the config from url
			try {
				URL configUrl = uri.toURL();
				URLConnection urlConnection = configUrl.openConnection();
				configStream = urlConnection.getInputStream();
			} catch (Exception ex) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Could not open a URLConnection to the file at: " + uri, ex);
				}
			}
		}
		return configStream;
	}

	private String findLatestBootableVersion(String bootableName) {
		String bootableVersion = "";
		for (BootableInfo bootable : bootables.values()) {
			if (bootableName.equals(bootable.getName())) {
				if (bootable.getVersion().compareTo(bootableVersion) > 0) {
					bootableVersion = bootable.getVersion();
				}
			}
		}
		return bootableVersion.trim().isEmpty() ? null : bootableVersion;
	}

	private JavaBootableLauncher getJavaBootableLauncher() {
		if (javaBootableLauncher == null) {
			javaBootableLauncher = new JavaBootableLauncher(getClassLoader());
			javaBootableLauncher.setBootConfigArgs(getBootConfigArgs());
		}
		return javaBootableLauncher;
	}

	private ScriptBootableLauncher getScriptBootableLauncher() {
		if (scriptBootableLauncher == null) {
			scriptBootableLauncher = new ScriptBootableLauncher(getClassLoader());
			scriptBootableLauncher.setBootConfigArgs(getBootConfigArgs());
		}
		return scriptBootableLauncher;
	}
}
