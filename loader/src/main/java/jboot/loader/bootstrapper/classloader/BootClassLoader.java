//J-
package jboot.loader.bootstrapper.classloader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jboot.loader.boot.model.Exclusion;
import jboot.loader.boot.node.ModelNode;
import jboot.loader.boot.node.ModelNodeDependency;
import jboot.loader.boot.node.resource.DirResource;
import jboot.loader.boot.node.resource.FileResource;
import jboot.loader.boot.node.resource.MultiResource;
import jboot.loader.boot.node.resource.Resource;
import jboot.loader.boot.resolver.ModelNodeResult;
import jboot.loader.bootstrapper.IBootstrapLoader;

public class BootClassLoader extends ClassLoader {
	private static Logger log = Logger.getLogger(BootClassLoader.class.getName());
	private static String osNameArch = "_" + System.getProperty("os.name").split(" ")[0].toLowerCase() + "_" + System.getProperty("os.arch").split(" ")[0].toLowerCase() + ".";
	private static long classLoaderId;
	private static long bootClassLoaderId;

	private static Field classesField;

	static {
		try {
			classesField = ClassLoader.class.getDeclaredField("classes");
			classesField.setAccessible(true);
		} catch (NoSuchFieldException ex) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Could not find the \"classes\" field in class ClassLoader. Lookup of dynamically defined classes will not work.", ex);
			}
		}
	}

	private long id;
	private IBootstrapLoader bootstrapLoader;
	private ModelNodeResult modelNodeResult;
	private Map<String, NodeClassLoader> nodeClassLoaders; //a map of all class loaders in the tree forest of classloaders. (maybe it's a graph forest because NodeClassLoader has a "List" of parents)
	private List<NodeClassLoader> rootNodeClassLoaders;
	private Map<URL, ProtectionDomain> protectionDomains;
	private File tmpLibDir;
	private Map<String, File> tmpLibs;
	private List<IClassLoaderListener> classLoaderListeners;
	private Set<String> localResources;
	private Set<String> localClasses;
	
	private Set<Integer> globalResourcesHashCodes;
	private List<Integer> tempResourcesHashCodes;
	private int [] sortedGlobalReourcesHashCodes;

	public BootClassLoader(ClassLoader parent, ModelNodeResult modelNodeResult, List<ModelNode> rootModelNodes, IBootstrapLoader bootstrapLoader, Set<String> localResources, Set<String> localClasses, Set<String> exclusions) throws Exception {
		super(parent);
		synchronized (BootClassLoader.class) {
			this.id = bootClassLoaderId++;
		}
		this.bootstrapLoader = bootstrapLoader;
		this.modelNodeResult = modelNodeResult;
		this.nodeClassLoaders = new LinkedHashMap<String, NodeClassLoader>();
		this.rootNodeClassLoaders = new ArrayList<NodeClassLoader>();
		if (localResources != null) {
			this.localResources = Collections.synchronizedSet(localResources);
		} else {
			this.localResources = Collections.emptySet();
		}
		if (localClasses != null) {
			this.localClasses = Collections.synchronizedSet(localClasses);
		} else {
			this.localClasses = Collections.emptySet();
		}
		for (ModelNode modelNode : rootModelNodes) {
		    if ((exclusions == null) || !exclusions.contains(modelNode.getGroupId() + ":" + modelNode.getArtifactId())) {
			NodeClassLoader nodeClassLoader=nodeClassLoaders.get(modelNode.getId());
			if (nodeClassLoader == null) {
				nodeClassLoader = new NodeClassLoader(modelNode, exclusions);
			}
			rootNodeClassLoaders.add(nodeClassLoader);
		    }
		}
		this.tmpLibs = new LinkedHashMap<String, File>();
		this.protectionDomains = new LinkedHashMap<URL, ProtectionDomain>();
		initTmpLibDir();
		if (log.isLoggable(Level.FINER)) {
			dumpClassLoader(new FileOutputStream("classloaders_" + id + ".xml"));
		} 
		classLoaderListeners = new Vector<IClassLoaderListener>();
		cacheResources();
	}

	public BootClassLoader(ModelNodeResult modelNodeResult, List<ModelNode> rootModelNodes, IBootstrapLoader bootstrapLoader, Set<String> localResources, Set<String> localClasses, Set<String> exclusions) throws Exception {
		this(BootClassLoader.class.getClassLoader(), modelNodeResult, rootModelNodes, bootstrapLoader, localResources, localClasses, exclusions);
	}
	
	private void initTmpLibDir() throws IOException {
		tmpLibDir = File.createTempFile("libclassloader", null);
		tmpLibDir.delete();
		tmpLibDir.mkdir();
		tmpLibDir.deleteOnExit();
	}

	public void addClassLoaderListener(IClassLoaderListener listener) {
		classLoaderListeners.add(listener);
	}

	public void removeClassLoaderListener(IClassLoaderListener listener) {
		classLoaderListeners.remove(listener);
	}
	
	//TODO: pass the certificates with which the jar pointed to by url was signed.
	private ProtectionDomain getProtectionDomain(URL url) {
		synchronized(protectionDomains){
			ProtectionDomain protectionDomain = null;
			if (url != null) {
				protectionDomain = (ProtectionDomain) protectionDomains.get(url);
				if (protectionDomain == null) {
					protectionDomain = new ProtectionDomain(new CodeSource(url, (java.security.cert.Certificate[]) null), new Permissions(), this, null);
					protectionDomains.put(url, protectionDomain);
				}
			}
			return protectionDomain;
		}
	}

	public IBootstrapLoader getBootstrapLoader() {
		return bootstrapLoader;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (isLocalClass(name)) {
			throw new ClassNotFoundException(name + " is flagged local. Root BootClassloader will not search for it.");
		}
		if (name != null && name.indexOf("?") >= 0) {
			return loadClassFromRequest(new ParsedRequest(name));
		}
		String classFilePath = name.replace('.', '/') + ".class";
		if(Arrays.binarySearch(sortedGlobalReourcesHashCodes, classFilePath.hashCode()) < 0) {
			if (log.isLoggable(Level.FINEST)) {
            	log.log(Level.FINEST, "Class " + name + " not found in the global cache of resources hash codes");
            }
			throw new ClassNotFoundException(name);
		}
		return findClass(new ClassVisitor(name, classFilePath));
	}

	private Class<?> findClass(ClassVisitor classVisitor) throws ClassNotFoundException {
		Class<?> clazz = null;
		for (NodeClassLoader nodeClassLoader : rootNodeClassLoaders) {
			clazz = nodeClassLoader.traverse(classVisitor);
			if (clazz != null) {
				return clazz;
			}
		}
		if (clazz == null) {
			throw new ClassNotFoundException(classVisitor.getName());
		}
		return clazz;
	}

	public Class<?> loadClassFromRequest(ParsedRequest req) throws ClassNotFoundException {
		if ( req.getBasePart() != null  && req.getGroupId() != null && req.getArtifactId() != null && req.getVersion() != null ) {
			String id = req.getGroupId() + ":" + req.getArtifactId() + ":" + req.getVersion();
			NodeClassLoader nodeClassLoader = nodeClassLoaders.get(id);
			if (nodeClassLoader != null) {
				String classFilePath = req.getBasePart().replace('.', '/') + ".class";
				try {
					return nodeClassLoader.findClassInNode(req.getBasePart(), classFilePath);
				} catch (Exception ex) {
					throw new ClassNotFoundException(req.getRequest(), ex);
				}
			}
		}
		throw new ClassNotFoundException(req.getRequest());
	}

	@Override
	protected URL findResource(String name) {
		ResourceVisitor resourceVisitor = new ResourceVisitor(name);
		ParsedRequest parsedRequest = resourceVisitor.getParsedRequest();
		if (isLocalResource(name) || (parsedRequest != null && parsedRequest.isLocal())) {
			return null;
		}
		if(parsedRequest == null && Arrays.binarySearch(sortedGlobalReourcesHashCodes, resourceVisitor.getName().hashCode()) < 0) {
			if (log.isLoggable(Level.FINEST)) {
            	log.log(Level.FINEST, "Resource " + name + " not found in the global cache of resources hash codes");
            }
			return null;
		}
		return findResource(new ResourceVisitor(name));
	}

	private URL findResource(ResourceVisitor resourceVisitor) {
		URL url = null;
		for (NodeClassLoader nodeClassLoader : rootNodeClassLoaders) {
			url = nodeClassLoader.traverse(resourceVisitor);
			if (url != null) {
				return url;
			}
		}
		return url;
	}

	private ClassLoader findResourceClassLoader(ClassLoaderVisitor classLoaderVisitor) {
		ClassLoader cl = null;
		for (NodeClassLoader nodeClassLoader : rootNodeClassLoaders) {
			cl = nodeClassLoader.traverse(classLoaderVisitor);
			if (cl != null) {
				return cl;
			}
		}
		return cl;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		Vector<URL> urls = new Vector<URL>();
		ResourcesVisitor resourcesVisitor = new ResourcesVisitor(name, urls);
		ParsedRequest parsedRequest = resourcesVisitor.getParsedRequest();
		if (isLocalResource(name) || (parsedRequest != null && parsedRequest.isLocal())) {
			return urls.elements();
		}
		findResources(resourcesVisitor);
		return urls.elements();
	}

	private void findResources(ResourcesVisitor resourcesVisitor) {
		if(resourcesVisitor.getParsedRequest() == null && Arrays.binarySearch(sortedGlobalReourcesHashCodes, resourcesVisitor.getName().hashCode()) < 0) {
			if (log.isLoggable(Level.FINEST)) {
            	log.log(Level.FINEST, "Resources " + resourcesVisitor.getName() + " not found in the global cache of resources hash codes");
            }
		}
		for (NodeClassLoader nodeClassLoader : rootNodeClassLoaders) {
			nodeClassLoader.traverse(resourcesVisitor);
		}
	}

	@Override
	protected String findLibrary(String libname) {
		return findLibrary(new LibraryVisitor(libname));
	}

	private String findLibrary(LibraryVisitor libraryVisitor) {
		for (NodeClassLoader nodeClassLoader : rootNodeClassLoaders) {
			String path = nodeClassLoader.traverse(libraryVisitor);
			if (path != null) {
				return path;
			}
		}
		return null;
	}

	public NodeClassLoader getNodeClassLoader(String id) {
		return nodeClassLoaders.get(id);
	}

	public StringBuffer dumpClassloaders() {//TODO maybe use XmlWriter???
		StringBuffer sb = new StringBuffer();
		sb.append("<classLoaderForest>\n");
		for (NodeClassLoader nodeClassLoader : rootNodeClassLoaders) {
			sb.append("\t<classLoaderTree>\n");
			DumpClassloaderVisitor dumpClassloaderVisitor = new DumpClassloaderVisitor("\t\t");
			nodeClassLoader.traverse(dumpClassloaderVisitor);
			sb.append(dumpClassloaderVisitor.getDump());
			sb.append("\t</classLoaderTree>\n");
		}
		sb.append("</classLoaderForest>\n");
		return sb;
	}

	public void dumpClassLoader(OutputStream os) throws IOException {
		try {
			StringBuffer sb = this.dumpClassloaders();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			osw.append(sb.toString());
			osw.flush();
		} finally {
			os.close();
		}
	}

	@Override
	public String toString() {
		return "BootClassLoader[Id: " + this.id + "] " + super.toString();
	}

	private boolean isLocalResource(String name) {
		for (String str : localResources) {
			if (name.endsWith(str)) {
				return true;
			}
		}
		return false;
	}

	private boolean isLocalClass(String name) {
		for (String str : localClasses) {
			if (name.endsWith(str)) {
				return true;
			}
		}
		return false;
	}

	public static class ParsedRequest {
		public static final String REGEX = "regex";
		public static final String LOCAL = "local";
		public static final String SWITCH_CCL = "switch.ccl";
		public static final String ROOT_URL = "root.url"; // ignores basePart and returns the root url of the resource of the nodeclassloader.
		// NOTE: when LOCAL is set to true REGEX and SWITCH_CCL are ignored.
		// NOTE: ROOT_URL==true is only useful when any of artifactId/groupId/version/packaging are specified. also, REGEX is ignored.
		// NOTE: ROOT_URL only works with getResource and getResources

		public static final String TRUE_VAL = "true";

		private String request;
		private String basePart;
		private String parametersPart;
		private String groupId;
		private String artifactId;
		private String version;
		private String packaging;
		private Pattern regex;
		private Boolean bRegex;
		private Boolean bLocal;
		private Boolean bSwitchCcl;
		private Boolean bRootURL;
		private Map<String, String> parameters;

		public ParsedRequest(String request) {
			this.request = request;
			parse();
			if (isRegex()) {
				this.regex = Pattern.compile(basePart);
			}
		}

		public static String[] parseParts(String request) {
			int splitIndex = request.indexOf('?');
			if (splitIndex < 0) {
				return new String[] { request, null };
			} else if (splitIndex < (request.length() - 1)) {
				return new String[] { request.substring(0, splitIndex), request.substring(splitIndex + 1) }; 
			} else { // splitIndex == request.length - 1
				return new String[] { request.substring(0, splitIndex), null };
			}
		}

		public static Map<String, String> parseParams(String parametersPart) {
			Map<String, String> paramsMap = new HashMap<String, String>();
			String[] args = parametersPart.split("&");
			for (String arg : args) {
				int equalsIndex = arg.indexOf('=');
				if (equalsIndex > 0 && equalsIndex < (arg.length()-1)) {
					String param = arg.substring(0, equalsIndex);
					String value = arg.substring(equalsIndex + 1);
					paramsMap.put(param, value);
				}
			}
			return paramsMap;
		}

		private void parse() {
			String parts[] = parseParts(request);

			basePart = parts[0];
			parametersPart = parts[1];

			if (parametersPart != null) {
				parameters = parseParams(parametersPart);
				groupId = parameters.get("groupid");
				artifactId = parameters.get("artifactid");
				version = parameters.get("version");
				packaging = parameters.get("packaging");
			} else {
				parameters = Collections.emptyMap();
			}
		}

		public boolean matches(NodeClassLoader ncl) {
			ModelNode modelNode = ncl.modelNode;
			if (groupId != null && !groupId.equals(modelNode.getGroupId())) {
				return false;
			}
			if (artifactId != null && !artifactId.equals(modelNode.getArtifactId())) {
				return false;
			}
			if (version != null && !version.equals(modelNode.getVersion())) {
				return false;
			}
			if (packaging != null && !packaging.equals(modelNode.getPackaging())) {
				return false;
			}
			return true;
		}

		public boolean isRegex() {
			if (bRegex == null) {
				bRegex = TRUE_VAL.equals(getParameters().get(REGEX));
			}
			return bRegex.booleanValue();
		}

		public boolean isLocal() {
			if (bLocal == null) {
				bLocal = TRUE_VAL.equals(getParameters().get(LOCAL));
			}
			return bLocal;
		}

		public boolean isSwitchCcl() {
			if (bSwitchCcl == null) {
				bSwitchCcl = TRUE_VAL.equals(getParameters().get(SWITCH_CCL));
			}
			return bSwitchCcl;
		}

		public boolean isRootURL() {
			if (bRootURL == null) {
				bRootURL = TRUE_VAL.equals(getParameters().get(ROOT_URL));
			}
			return bRootURL;
		}

		public String getRequest() {
			return request;
		}

		public String getBasePart() {
			return basePart;
		}

		public String getParametersPart() {
			return parametersPart;
		}

		public String getGroupId() {
			return groupId;
		}

		public String getArtifactId() {
			return artifactId;
		}

		public String getVersion() {
			return version;
		}

		public String getPackaging() {
			return packaging;
		}

		public Pattern getRegex() {
			return regex;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}
	}

	public static interface IVisitor<T> {
		public T visit(NodeClassLoader nodeClassLoader);
	}

	private abstract class AbstractVisitor<T> implements IVisitor<T> {
		private Deque<NodeClassLoader> deque;
		private Set<Long> visited; // items are ModelNode ids.

		public AbstractVisitor() {
			deque = new LinkedList<NodeClassLoader>();
			visited = new HashSet<Long>();
		}

		public void enqueue(NodeClassLoader... ncls) {
			for (NodeClassLoader ncl : ncls) {
				Long id = ncl.getId();
				if (!visited.contains(id)) {
					visited.add(id);
					deque.offerLast(ncl);
				}
			}
		}

		public void clearQueue() {
			deque.clear();
		}

		public T traverse(NodeClassLoader ncl) {
			clearQueue();
			enqueue(ncl);
			return traverse();
		}

		public T traverse() {
			NodeClassLoader ncl = null;
			while ((ncl = deque.pollFirst()) != null) {
				T t = visit(ncl);
				if (t != null) {
					return t;
				}
				enqueue(ncl.getChildren());
			}
			return null;
		}
	}

	private class ClassVisitor extends AbstractVisitor<Class<?>> {
		private String name;
		private String classFilePath;

		public ClassVisitor(String name, String classFilePath) {
			this.name = name;
			this.classFilePath = classFilePath;
		}

		public String getName() {
			return name;
		}

		public String getClassFilePath() {
			return classFilePath;
		}

		public Class<?> visit(NodeClassLoader nodeClassLoader) {
			try {
				Class<?> clazz = nodeClassLoader.findClassInNode(name, classFilePath);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Class " + name + (clazz == null ? " not" : "") + " found in " + nodeClassLoader);
				}
				return clazz;
			} catch (Exception ex) {
				if (log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Error while loading class " + name + " in " + nodeClassLoader, ex);
				}
			}
			return null;
		}
	}

	private class ResourceVisitor extends AbstractVisitor<URL> {
		private String name;
		private ParsedRequest request;

		public ResourceVisitor(String name) {
			if (name != null && name.indexOf('?') >= 0) {
				this.request = new ParsedRequest(name);
				this.name = request.getBasePart();
			} else {
				this.name = name;
			}
		}

		public String getName() {
			return name;
		}

		public ParsedRequest getParsedRequest() {
			return request;
		}

		public URL visit(NodeClassLoader nodeClassLoader) {
			try {
				URL url = null;
				if (request != null) {
					if (request.matches(nodeClassLoader)) {
						url = nodeClassLoader.findResourceInNode(request);
					}
				} else {
					url = nodeClassLoader.findResourceInNode(name);
				}
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Resource " + name + (url == null ? " not" : "") + " found in " + nodeClassLoader + (url == null ? "" : " ( " + url + " )") + (request == null ? "" : " with request ( " + request.getRequest() + " )"));
				}
				if (request != null && url != null && request.isSwitchCcl()) {
					Thread.currentThread().setContextClassLoader(nodeClassLoader);
				}
				return url;
			} catch (Exception ex) {
				if (log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Error while loading resource " + name + " in " + nodeClassLoader + (request == null ? "" : " with request ( " + request.getRequest() + " )"), ex);
				}
			}
			return null;
		}
	}

	private class ResourcesVisitor extends AbstractVisitor<URL> {
		private String name;
		private ParsedRequest request;

		private List<URL> urls;

		public ResourcesVisitor(String name, List<URL> urls) {
			if (name != null && name.indexOf('?') >= 0) {
				this.request = new ParsedRequest(name);
				this.name = request.getBasePart();
			} else {
				this.name = name;
			}
			this.urls = urls;
		}

		public String getName() {
			return name;
		}

		public ParsedRequest getParsedRequest() {
			return request;
		}

		public URL visit(NodeClassLoader nodeClassLoader) { //always returns null in order to visit all nodes. See NodeClassLoader.traverse
			try {
				List<URL> foundUrls = null;
				if (request != null) {
					if (request.matches(nodeClassLoader)) {
						foundUrls = nodeClassLoader.findResourcesInNode(request);
					}
				} else {
					foundUrls = nodeClassLoader.findResourcesInNode(name);
				}
				if (foundUrls != null) {
					for (URL url : foundUrls) {
						if (!urls.contains(url)) {
							urls.add(url);
						}
					}
				}
				if (log.isLoggable(Level.FINEST)) {
					String strUrls = "";
					if (foundUrls != null) {
						for (int i=0;i<foundUrls.size();i++) {
							strUrls += foundUrls.get(i).toString();
							if (i < foundUrls.size()-1) {
								strUrls += ", ";
							}
						}
					}
					log.log(Level.FINEST, "Resources of " + name + (foundUrls == null ? " not" : "") + " found in " + nodeClassLoader + (foundUrls == null ? "" : " ( " + strUrls + " )") + (request == null ? "" : " with request ( " + request.getRequest() + " )"));
				}
			} catch (Exception ex) {
				if (log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Error while loading resource " + name + " in " + nodeClassLoader + (request == null ? "" : " with request ( " + request.getRequest() + " )"), ex);
				}
			}
			return null;
		}
	}

	private class LibraryVisitor extends AbstractVisitor<String> {
		private String name;

		public LibraryVisitor(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String visit(NodeClassLoader nodeClassLoader) {
			try {
				String path = nodeClassLoader.findLibraryInNode(name);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Library " + name + (path == null ? " not" : "") + " found in " + nodeClassLoader + (path == null ? " " : " ( " + path + " )"));
				}
				return path;
			} catch (Exception ex) {
				if (log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Error while loading library " + name + " in " + nodeClassLoader, ex);
				}
			}
			return null;
		}
	}

	private class ClassLoaderVisitor extends AbstractVisitor<ClassLoader> {
		private String name;
		private ParsedRequest request;

		public ClassLoaderVisitor(String name) {
			if (name != null && name.indexOf("?") >= 0) {
				this.request = new ParsedRequest(name);
				this.name = request.getBasePart();
			} else {
				this.name = name;
			}
		}

		public String getName() {
			return name;
		}

		public ParsedRequest getParsedRequest() {
			return request;
		}

		public ClassLoader visit(NodeClassLoader nodeClassLoader) {
			try {
				URL url = null;
				if (request != null) {
					if (request.matches(nodeClassLoader)) {
						url = nodeClassLoader.findResourceInNode(request);
					}
				} else {
					url = nodeClassLoader.findResourceInNode(name);
				}
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Resource " + name + (url == null ? " not" : "") + " found in " + nodeClassLoader + (url == null ? "" : " ( " + url + " )") + (request == null ? "" : " with request ( " + request.getRequest() + " )"));
				}
				return url != null ? nodeClassLoader : null;
			} catch (Exception ex) {
				if (log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Error while loading resource " + name + " in " + nodeClassLoader + (request == null ? "" : " with request ( " + request.getRequest() + " )"), ex);
				}
			}
			return null;
		}
	}

	private class DumpClassloaderVisitor extends AbstractVisitor<Object> {
		private StringBuffer sb;
		private String indent;

		public DumpClassloaderVisitor(String indent) {
			this.sb = new StringBuffer();
			this.indent = indent;
		}

		public StringBuffer getDump() {
			return sb;
		}

		public Object visit(NodeClassLoader nodeClassLoader) { // always returns null in order to visit all nodes. See NodeClassLoader.traverse
			sb.append(indent); sb.append("<nodeClassLoader>\n");
			sb.append(indent); sb.append("\t<id>"); sb.append(nodeClassLoader.nodeClassLoaderId); sb.append("</id>\n");
			sb.append(indent); sb.append("\t<dependency>"); sb.append(nodeClassLoader.getModelId()); sb.append("</dependency>\n");
			serializeResource(nodeClassLoader.resource, sb, indent + "\t");
			sb.append(indent); sb.append("\t<parents>\n");
			for (NodeClassLoader parent : nodeClassLoader.parentNodeClassLoaders) {
				sb.append(indent); sb.append("\t\t<parentRef>"); sb.append(parent.nodeClassLoaderId); sb.append("</parentRef>\n");
			}
			sb.append(indent); sb.append("\t</parents>\n");
			sb.append(indent); sb.append("\t<children>\n");
			for (NodeClassLoader child : nodeClassLoader.getChildren()) {
				sb.append(indent); sb.append("\t\t<childRef>"); sb.append(child.nodeClassLoaderId); sb.append("</childRef>\n");
			}
			sb.append(indent); sb.append("\t</children>\n");
			sb.append(indent); sb.append("</nodeClassLoader>\n");
			return null;
		}

		private void serializeResource(Resource res, StringBuffer sb, String indent) {
			sb.append(indent); sb.append("<resource type=\""); sb.append(res.getClass().getName()); sb.append("\">\n");
			if (res instanceof FileResource) {
				sb.append(indent + "\t"); sb.append("<artifact>"); sb.append(((FileResource)res).getFile().getAbsolutePath()); sb.append("</artifact>"); sb.append("\n");
//				if (res instanceof JarResource) {
//					JarResource jarRes = (JarResource) res;
//					sb.append(indent + "\t"); sb.append("<zipEntryFetches>"); sb.append(jarRes.getZipEntryFetches()); sb.append("</zipEntryFetches>"); sb.append("\n");
//					sb.append(indent + "\t"); sb.append("<zipEntryByteFetches>"); sb.append(jarRes.getZipEntryByteFetches()); sb.append("</zipEntryByteFetches>"); sb.append("\n");
//					if (res instanceof CachedJarResource) {
//						CachedJarResource cachedJarRes = (CachedJarResource) res;
//						sb.append(indent + "\t"); sb.append("<urlCacheSize>"); sb.append(cachedJarRes.getUrlCacheSize()); sb.append("</urlCacheSize>"); sb.append("\n");
//						sb.append(indent + "\t"); sb.append("<urlCacheHits>"); sb.append(cachedJarRes.getUrlCacheHits()); sb.append("</urlCacheHits>"); sb.append("\n");
//						sb.append(indent + "\t"); sb.append("<urlCacheMisses>"); sb.append(cachedJarRes.getUrlCacheMisses()); sb.append("</urlCacheMisses>"); sb.append("\n");
//						sb.append(indent + "\t"); sb.append("<unavailableEntriesCacheSize>"); sb.append(cachedJarRes.getUnavailableEntriesCacheSize()); sb.append("</unavailableEntriesCacheSize>"); sb.append("\n");
//						sb.append(indent + "\t"); sb.append("<unavailableEntriesCacheHits>"); sb.append(cachedJarRes.getUnavailableCacheHits()); sb.append("</unavailableEntriesCacheHits>"); sb.append("\n");
//						sb.append(indent + "\t"); sb.append("<unavailableEntriesCacheMisses>"); sb.append(cachedJarRes.getUnavailableCacheMisses()); sb.append("</unavailableEntriesCacheMisses>"); sb.append("\n");
//					}
//				}
			} else if (res instanceof DirResource) {
				sb.append(indent + "\t"); sb.append("<artifact>"); sb.append(((DirResource)res).getFile().getAbsolutePath()); sb.append("</artifact>"); sb.append("\n");
			} else if (res instanceof MultiResource) {
				for (Resource innerRes : ((MultiResource)res).getResources()) {
					serializeResource(innerRes, sb, indent + "\t");
				}
			} else {
				sb.append(indent); sb.append("\t<artifact>Unknown Resource Type</artifact>\n");				
			}
			sb.append(indent); sb.append("</resource>\n");
		}
	}

	public class NodeClassLoader extends ClassLoader {
		private Long nodeClassLoaderId;
		private ModelNode modelNode;
		private Map<String, NodeClassLoader> childrenNodeClassLoaders;
		private NodeClassLoader [] childrenNodeClassLoadersArray;
		private List<NodeClassLoader> parentNodeClassLoaders;
		private Map<String, Class<?>> classes;
		private List<Class<?>> jvmClasses;
		private Resource resource;
		private String cachedModelId;
		private int [] sortedAllReourcesHashCodes;

		@SuppressWarnings("unchecked")
		protected NodeClassLoader(ModelNode modelNode, Set<String> exclusions) throws Exception {
			synchronized (BootClassLoader.class) {
				this.nodeClassLoaderId = Long.valueOf(BootClassLoader.classLoaderId++);
			}
			this.modelNode = modelNode;
			this.cachedModelId = modelNode.getId();
			BootClassLoader.this.nodeClassLoaders.put(cachedModelId, this);
			this.childrenNodeClassLoaders = Collections.synchronizedMap(new LinkedHashMap<String, NodeClassLoader>());
			childrenNodeClassLoadersArray = new NodeClassLoader [0];
			this.parentNodeClassLoaders = new Vector<NodeClassLoader>();
			this.classes = new Hashtable<String, Class<?>>();
			if (classesField != null) {
				this.jvmClasses = (List<Class<?>>)classesField.get(this);
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Creating classloader " + this);
			}
			initArtifactFiles();
			if (exclusions == null) {
				exclusions = new HashSet<String>();
			}
			List<ModelNodeDependency> modelNodeDependencies = modelNode.getResolvedModelNodeDependency();
			if (modelNodeDependencies != null) {
				for (ModelNodeDependency modelNodeDependency : modelNodeDependencies) {
					if (!exclusions.contains(modelNodeDependency.getGroupId() + ":" + modelNodeDependency.getArtifactId())) {
						NodeClassLoader nodeClassLoader = null;
						if (!BootClassLoader.this.nodeClassLoaders.containsKey(modelNodeDependency.getId())) {
							nodeClassLoader = new NodeClassLoader(BootClassLoader.this.modelNodeResult.getModelNode(modelNodeDependency.getId()), getDependencyExclusions(modelNodeDependency, exclusions));
						} else {
							nodeClassLoader = BootClassLoader.this.nodeClassLoaders.get(modelNodeDependency.getId());
							nodeClassLoader.excludeChildren(getDependencyExclusions(modelNodeDependency, exclusions), new HashSet<Long>());
						}
						addChild(nodeClassLoader);
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Dependency " + modelNodeDependency.getId() + " is excluded from classloader " + this);
						}
					}
				}
			}
		}

		public Long getId() {
			return nodeClassLoaderId;
		}

		public String getModelId() {
			return cachedModelId;
		}

		public IBootstrapLoader getBootstrapLoader() {
			return BootClassLoader.this.bootstrapLoader;
		}

		public NodeClassLoader[] getChildren() {
			// The below synchronization was added because a deadlock was occuring at parrallel loading of the same class
			NodeClassLoader[] tmp;
			synchronized (nodeClassLoaderId) {
				tmp = childrenNodeClassLoadersArray;
			}
			return tmp;
		}

		protected void cacheResourcesHashCode() {
			tempResourcesHashCodes.clear();
			try {
				Iterator<String> iterator = resource.getAllEntriesIncludingFolders();
				while(iterator.hasNext()) {
					String next = iterator.next();
					if(next.endsWith("/")) {
						next = next.substring(0, next.length() - 1);
					}
					int hashCode = next.hashCode();
					tempResourcesHashCodes.add(hashCode);
					globalResourcesHashCodes.add(hashCode);
				}
			} catch (Exception ex) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Error while caching the resources of " + this, ex);
				}
				throw new RuntimeException(ex);
			}
			sortedAllReourcesHashCodes = new int[tempResourcesHashCodes.size()];
			for(int i = 0 ; i < sortedAllReourcesHashCodes.length; i++) {
				sortedAllReourcesHashCodes[i] = tempResourcesHashCodes.get(i).intValue();
			}
			Arrays.sort(sortedAllReourcesHashCodes);
		}
		
		private synchronized boolean addChild(NodeClassLoader nodeClassLoader) {
			if (childrenNodeClassLoaders.put(nodeClassLoader.getModelId(), nodeClassLoader) == null) {
				updateChildrenNodeClassLoadersArray();
				nodeClassLoader.parentNodeClassLoaders.add(this);
				return true;
			}
			updateChildrenNodeClassLoadersArray();
			return false;
		}
		
		private synchronized void updateChildrenNodeClassLoadersArray() {
			NodeClassLoader[] tmp = childrenNodeClassLoaders.values().toArray(new NodeClassLoader[childrenNodeClassLoaders.size()]);
			synchronized (nodeClassLoaderId) {
				childrenNodeClassLoadersArray = tmp;
			}
		}

		private void initArtifactFiles() {
			resource = modelNode.getArtifact(null).getResource();
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Artifact of " + this + " is " + resource);
			}
		}

		private Set<String> getDependencyExclusions(ModelNodeDependency modelNodeDependency, Set<String> exclusions) {
			Set<String> result = new HashSet<String>();
			if (modelNodeDependency.getDependency().getExclusions() != null) {
				for (Exclusion exclusion : modelNodeDependency.getDependency().getExclusions().getExclusion()) {
					result.add(exclusion.getGroupId() + ":" + exclusion.getArtifactId());
				}
			}
			result.addAll(exclusions);
			return result;
		}

		private void excludeChildren(Set<String> exclusions,Set<Long> ids) {
			if (!exclusions.isEmpty()) {
				Iterator<NodeClassLoader> iterator = childrenNodeClassLoaders.values().iterator();
				while (iterator.hasNext()) {
					NodeClassLoader childNodeClassLoader = iterator.next();
					if (exclusions.contains(childNodeClassLoader.modelNode.getGroupId() + ":" + childNodeClassLoader.modelNode.getArtifactId())) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Dependency " + childNodeClassLoader.getModelId() + " is excluded from classloader " + this);
						}
						childNodeClassLoader.parentNodeClassLoaders.remove(this);
						iterator.remove();
					} else {
					    if (!ids.contains(childNodeClassLoader.getId())){
                            ids.add(childNodeClassLoader.getId());
                            childNodeClassLoader.excludeChildren(exclusions,ids);
                        }
					}
					updateChildrenNodeClassLoadersArray();
				}
			}
		}

		protected <T> T traverse(AbstractVisitor<T> visitor) {
			return visitor.traverse(this);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			Class<?> clazz = null;

			//first look in parents of the root BootClassLoader
			ClassLoader parent = BootClassLoader.this.getParent();
			if (parent != null) {
				try {
                    clazz = parent.loadClass(name);
				} catch (ClassNotFoundException ex) {
                    if (log.isLoggable(Level.FINEST)) {
                    	log.log(Level.FINEST, "Class " + name + " not found in parent classloader " + parent + " of classloader " + this, ex);
                    }
				}
			}
			if (clazz == null) {
                clazz = findClass(name);
			}
			if (resolve) {
				resolveClass(clazz);
			}
			if (log.isLoggable(Level.FINER)) {
				ClassLoader cl = clazz.getClassLoader();
				log.log(Level.FINER, "Class " + name + " requested from " + this + " found in " + (cl != null ? cl : "[system]"));
			}
			return clazz;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			Class<?> clazz = null;
			String classFilePath = name.replace('.', '/') + ".class";
			if (isLocalClass(name)) {
				try {
					clazz = findClassInNode(name, classFilePath);
				} catch (Throwable th) {
					throw new ClassNotFoundException("Local class " + name + " not found.", th);
				}
			} else {
				if(Arrays.binarySearch(sortedGlobalReourcesHashCodes, classFilePath.hashCode()) < 0) {
					if (log.isLoggable(Level.FINEST)) {
		            	log.log(Level.FINEST, "Class " + name + " not found in the global cache of resources hash codes");
		            }
					throw new ClassNotFoundException(name);
				}
				ClassVisitor classVisitor = new ClassVisitor(name, classFilePath);
				clazz = traverse(classVisitor); //search this node and its children
				if (clazz == null) {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Class " + name + " not found in " + this + " or in any of its descendants.");
					}
					clazz = findClass(classVisitor); //search this node's parents, their children, and finally the whole classloader forest.
				}
				if (clazz != null) {
					ClassLoader cl = clazz.getClassLoader();
					if (cl instanceof NodeClassLoader && cl != this) {
						if (this.addChild((NodeClassLoader) cl)) {
							if (log.isLoggable(Level.FINER)) {
								log.log(Level.FINER, "Adding " + cl + " as child of " + this + " after loading " + clazz.getName());
							}
						}
					}
				}
			}
			if (clazz == null) {
				throw new ClassNotFoundException(name);
			}
			return clazz;
		}

		protected Class<?> findClassInNode(String name, String classFilePath) throws Exception {
			if(sortedAllReourcesHashCodes != null && Arrays.binarySearch(sortedAllReourcesHashCodes, classFilePath.hashCode()) >= 0) {
				Class<?> clazz = classes.get(name);
				if (clazz != null) {
					return clazz;
				} else if (jvmClasses != null) {
					if (jvmClasses.size() != classes.size()) {
						if (log.isLoggable(Level.FINER)) {
							log.log(Level.FINER, "Jvm loaded classes different. reconciling " + this);
						}
						for (Class<?> clazz0 : jvmClasses) {
							classes.put(clazz0.getName(), clazz0);
						}
						clazz = classes.get(name);
						if (clazz != null) {
							return clazz;
						}
					}
				}
				byte[] bytecode = resource.getEntryBytes(classFilePath);
				if (bytecode != null) {
					URL url = resource.getEntryRoot(classFilePath); //should not return null because bytecode is not null.
					ProtectionDomain protectionDomain = BootClassLoader.this.getProtectionDomain(url);

					//building the class
					int index = name.lastIndexOf('.');
					if (index != -1) {
						String pkgname = name.substring(0, index);
						Package pkg = getPackage(pkgname);
						if (pkg == null) {
							definePackage(pkgname, null, null, null, null, null, null, null);
						}
					}
					for (IClassLoaderListener listener : classLoaderListeners) {
						listener.preClassLoaded(this, name, bytecode);
					}
                    synchronized(NodeClassLoader.this){//to avoid linkageerror due to loading in parallel
                        clazz=classes.get(name);
                        if (clazz==null){
                            clazz = defineClass(name, bytecode, 0, bytecode.length, protectionDomain);
                            classes.put(name, clazz);
                        }
                    }
					for (IClassLoaderListener listener : classLoaderListeners) {
						listener.postClassLoaded(this, name, clazz, bytecode);
					}
					return clazz;
				}
			} else {
				if (log.isLoggable(Level.FINEST)) {
	            	log.log(Level.FINEST, "Class " + name + " not found in the local cache of resources hash codes for " + this);
	            }
			}
			return null;
		}

		private Class<?> findClass(ClassVisitor classVisitor) {
			//look in direct parents and their children
			for (NodeClassLoader parent : parentNodeClassLoaders.toArray(new NodeClassLoader[0])) {
				Class<?> clazz = parent.traverse(classVisitor);
				if (clazz != null) {
					return clazz;
				}
			}
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Class " + classVisitor.getName() + " not found in parents of " + this + " or in any of their descendants.");
			}
			//look in all root classloaders.
			try {
				Class<?> clazz = BootClassLoader.this.findClass(classVisitor);
				if (clazz != null) {
					return clazz;
				}
			} catch (Exception ex) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, ex.toString(), ex);
				}
			}
			return null;
		}

		@Override
		public URL getResource(String name) {
			URL url = null;
			String parts[] = null;
			//first look in parent of the root BootClassLoader
			ClassLoader parent = BootClassLoader.this.getParent();
			if (parent != null) {
				parts = ParsedRequest.parseParts(name);
				url = parent.getResource(parts[0]);
			}
			if (url == null) {
                url = findResource(name);
			} else if (parts != null && parts[1] != null
						&& ParsedRequest.TRUE_VAL.equals(ParsedRequest.parseParams(parts[1]).get(ParsedRequest.SWITCH_CCL))) {
				Thread.currentThread().setContextClassLoader(parent);
			}
			return url;
		}

		@Override
		protected URL findResource(String name) {			
			URL url = null;
			ResourceVisitor resourceVisitor = new ResourceVisitor(name);
			ParsedRequest parsedRequest = resourceVisitor.getParsedRequest();
			if (isLocalResource(name) || (parsedRequest != null && parsedRequest.isLocal())) {
				try {
					url = findResourceInNode(parsedRequest != null ? parsedRequest.getBasePart() : name);
				} catch (Throwable th) {
					throw new RuntimeException("Error while finding resource " + name, th);
				}
			} else {
				if(parsedRequest == null && Arrays.binarySearch(sortedGlobalReourcesHashCodes, resourceVisitor.getName().hashCode()) < 0) {
					if (log.isLoggable(Level.FINEST)) {
		            	log.log(Level.FINEST, "Resource " + name + " not found in the global cache of resources hash codes");
		            }
					return null;
				}
				url = traverse(resourceVisitor);
				if (url == null) {
					try {
						url = findResource(resourceVisitor);
					} catch (Throwable th) {
						throw new RuntimeException("Error while finding resource " + name, th);
					}
				}
			}
			return url;
		}

		public URL findResourceInNode(String name) throws Exception {
			if(sortedAllReourcesHashCodes != null && Arrays.binarySearch(sortedAllReourcesHashCodes, name.hashCode()) >= 0) {
				List<URL> urls = resource.getEntry(name);
				if (urls != null) {
					return urls.get(0);
				}
			} else {
				if (log.isLoggable(Level.FINEST)) {
	            	log.log(Level.FINEST, "Resource " + name + " not found in the local cache of resources hash codes for " + this);
	            }
			}
			return null;
		}

		public URL findResourceInNode(ParsedRequest request) throws Exception {
			if (request.isRegex()) {
				Iterator<String> iter = resource.getAllEntries();
				while (iter.hasNext()) {
					String item = iter.next();
					if (request.getRegex().matcher(item).matches()) {
						return findResourceInNode(item);
					}
				}
			} else if (request.isRootURL()) {
				if (resource instanceof FileResource) {
					return ((FileResource) resource).getFileURL();
				} else {
					Iterator<String> iter = resource.getAllEntries();
					if (iter.hasNext()) {
						String firstEntry = iter.next();
						return resource.getEntryRoot(firstEntry);
					}
				}
			} else {
				return findResourceInNode(request.getBasePart());
			}
			return null;
		}

		private URL findResource(ResourceVisitor resourceVisitor) throws Exception {
			for (NodeClassLoader parent : parentNodeClassLoaders.toArray(new NodeClassLoader[0])) {
				URL url = parent.traverse(resourceVisitor);
				if (url != null) {
					return url;
				}
			}
			return BootClassLoader.this.findResource(resourceVisitor);
		}

		public ClassLoader getResourceClassLoader(String name) {
			URL url = null;
			//first look in parent of the root BootClassLoader
			ClassLoader parent = BootClassLoader.this.getParent();
			if (parent != null) {
				url = parent.getResource(name);
				if (url != null) {
					return parent;
				}
			}
			return findResourceClassLoader(name);
		}

		protected ClassLoader findResourceClassLoader(String name) {
			ClassLoader cl = null;
			ClassLoaderVisitor classLoaderVisitor = new ClassLoaderVisitor(name);
			if(classLoaderVisitor.getParsedRequest() == null && Arrays.binarySearch(sortedGlobalReourcesHashCodes, name.hashCode()) < 0) {
				if (log.isLoggable(Level.FINEST)) {
	            	log.log(Level.FINEST, "ClassLoader for Resource " + name + " not found in the global cache of resources hash codes");
	            }
				return null;
			}
			cl = traverse(classLoaderVisitor);
			if (cl == null) {
				cl = findResourceClassLoader(classLoaderVisitor);
			}
			return cl;
		}

		private ClassLoader findResourceClassLoader(ClassLoaderVisitor classLoaderVisitor) {
			for (NodeClassLoader parent : parentNodeClassLoaders.toArray(new NodeClassLoader[0])) {
				ClassLoader cl = parent.traverse(classLoaderVisitor);
				if (cl != null) {
					return cl;
				}
			}
			return BootClassLoader.this.findResourceClassLoader(classLoaderVisitor);
		}

        @Override
		public Enumeration<URL> getResources(String name) throws IOException {
			Vector<URL> urls = new Vector<URL>();

			Enumeration<URL> parentUrls = null;
			ClassLoader parent = BootClassLoader.this.getParent();
			if (parent != null) {
				String parts[] = ParsedRequest.parseParts(name);
				parentUrls = parent.getResources(parts[0]);
			}
			if (parentUrls != null) {
				while (parentUrls.hasMoreElements()) {
					URL url = parentUrls.nextElement();
					if (!urls.contains(url)) {
						urls.add(url);
					}
				}
			}

			Enumeration<URL> foundUrls = findResources(name);
			if (foundUrls != null) {
				while (foundUrls.hasMoreElements()) {
					URL url = foundUrls.nextElement();
					if (!urls.contains(url)) {
						urls.add(url);
					}
				}
			}

			return urls.elements();
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			Vector<URL> urls = new Vector<URL>();
			ResourcesVisitor resourcesVisitor = new ResourcesVisitor(name, urls);
			ParsedRequest parsedRequest = resourcesVisitor.getParsedRequest();
			if (isLocalResource(name) || (parsedRequest != null && parsedRequest.isLocal())) {
				try {
					List<URL> localUrls = findResourcesInNode(parsedRequest != null ? parsedRequest.getBasePart() : name);
					if(localUrls != null) {
						urls.addAll(localUrls);
					}
				} catch (Throwable th) {
					throw new RuntimeException("Error while loading resource " + name, th);
				}
			} else {
				BootClassLoader.this.findResources(resourcesVisitor); //immediately look in all of the BootClassLoader, because we want everything.
			}
			return urls.elements();
		}

		public List<URL> findResourcesInNode(String name) throws Exception {
			if(sortedAllReourcesHashCodes != null && Arrays.binarySearch(sortedAllReourcesHashCodes, name.hashCode()) >= 0) {
				return resource.getEntry(name);
			} else {
				if (log.isLoggable(Level.FINEST)) {
	            	log.log(Level.FINEST, "Resources " + name + " not found in the local cache of resources hash codes for " + this);
	            }
			}
			return null;
		}

		public List<URL> findResourcesInNode(ParsedRequest request) throws Exception {
			List<URL> urls = null;
			if (request.isRegex()) {
				Iterator<String> iter = resource.getAllEntries();
				while (iter.hasNext()) {
					String item = iter.next();
					if (request.getRegex().matcher(item).matches()) {
						if (urls == null) {
							urls = new Vector<URL>(); 
						}
						List<URL> findResourcesInNode = findResourcesInNode(item);
						if(findResourcesInNode != null) {
							urls.addAll(findResourcesInNode);
						}
					}
				}
			} else if (request.isRootURL()) {
				if (urls == null) {
					urls = new Vector<URL>(); 
				}
				if (resource instanceof FileResource) {
					urls.add(((FileResource) resource).getFileURL());
				} else {
					Iterator<String> iter = resource.getAllEntries();
					if (iter.hasNext()) {
						String firstEntry = iter.next();
						urls.add(resource.getEntryRoot(firstEntry));
					}
				}
			} else {
				urls = findResourcesInNode(request.getBasePart());
			}
			return urls;
		}

		@Override
		protected String findLibrary(String libname) {
			LibraryVisitor libraryVisitor = new LibraryVisitor(libname);
			String path = traverse(libraryVisitor);
			if (path != null) {
				return path;
			}
			for (NodeClassLoader parent : parentNodeClassLoaders.toArray(new NodeClassLoader[0])) {
				path = parent.traverse(libraryVisitor);
				if (path != null) {
					return path;
				}
			}
			return BootClassLoader.this.findLibrary(libraryVisitor);
		}

		public String findLibraryInNode(String libname) throws Exception {
			Iterator<String> entriesIter = resource.getAllEntries();
			while (entriesIter.hasNext()) {
				String entry = entriesIter.next();
				String fileName = null;
				int slashIndex = entry.lastIndexOf('/');
				fileName = entry.substring(slashIndex > 0 ? slashIndex + 1 : 0);
				if (fileName.startsWith(libname + osNameArch) ||
					fileName.startsWith("lib" + libname + osNameArch) ||
					fileName.startsWith(libname + ".") ||
					fileName.startsWith("lib" + libname + ".")) {
					byte[] bytes = resource.getEntryBytes(entry);
					return getTmpFile(bytes, entry, libname);
				}
			}
			return null;
		}

		private synchronized String getTmpFile(byte[] bytes, String entry, String libname) throws IOException {
			File tmpFile = tmpLibs.get(libname);
			if (tmpFile == null) {
				tmpFile = new File(tmpLibDir, entry);
				tmpFile.getParentFile().mkdirs();
				DataOutputStream dataOutputStream = null;
				try {
					dataOutputStream = new DataOutputStream(new FileOutputStream(tmpFile));
					dataOutputStream.write(bytes);
				} finally {
					if (dataOutputStream != null) {
						dataOutputStream.flush();
						dataOutputStream.close();
					}
				}
				tmpLibs.put(libname, tmpFile);
				tmpFile.deleteOnExit();
			}
			return tmpFile.getAbsolutePath();
		}

		@Override
		public String toString() {
			return "NodeClassLoader[Id: " + nodeClassLoaderId + "][Artifact: " + getModelId() + "]";
		}
	}

	private class ResourceCacherVisitor extends AbstractVisitor<String> {
		
		public ResourceCacherVisitor() {
			super();
		}

		@Override
		public String visit(NodeClassLoader nodeClassLoader) {
			nodeClassLoader.cacheResourcesHashCode();
			return null;
		}
		
	}
	

	public int [] getGlobalAllResourcesHashCodes() {
		return sortedGlobalReourcesHashCodes;
	}
	
	private void cacheResources() {
		long startTime = System.currentTimeMillis();
		globalResourcesHashCodes = new HashSet<Integer>();
		tempResourcesHashCodes = new ArrayList<Integer>();
		ResourceCacherVisitor visitor = new ResourceCacherVisitor();
		for (NodeClassLoader nodeClassLoader : rootNodeClassLoaders) {
			nodeClassLoader.traverse(visitor);
		}
		tempResourcesHashCodes = null;
		sortedGlobalReourcesHashCodes = new int[globalResourcesHashCodes.size()];
		int index = 0;
		for(Iterator<Integer> iterator = globalResourcesHashCodes.iterator(); iterator.hasNext();) {			
			sortedGlobalReourcesHashCodes[index++] = iterator.next().intValue();
		}
		Arrays.sort(sortedGlobalReourcesHashCodes);
		globalResourcesHashCodes = null;
		long endTime = System.currentTimeMillis();
		if(log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, String.format("Resources hash codes have been cached in %d ms. Global Caches number of entries %d", (endTime - startTime), sortedGlobalReourcesHashCodes.length));
		}
	}
	
	public static interface IClassLoaderListener {
		public void preClassLoaded(NodeClassLoader nodeClassLoader,String className, byte[] bytes);
		public void postClassLoaded(NodeClassLoader nodeClassLoader,String className, Class<?> clazz, byte[] bytes);
	}

}
//J+
