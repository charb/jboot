package jboot.loader.boot.node.resource;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public abstract class Resource {
    public static final String JBOOT_RESOURCE_DAEMON_ENABLED = "jboot.jarResourceDaemon.enabled";
	private static Logger log = Logger.getLogger(Resource.class.getName());

	private static JarResourceDaemon jarResourceDaemon = null;
	
	static {
	    String strLaunchDaemon = System.getProperty(JBOOT_RESOURCE_DAEMON_ENABLED);
	    if (strLaunchDaemon == null || strLaunchDaemon.equals("true")) {
    	    jarResourceDaemon = new JarResourceDaemon();
    	    jarResourceDaemon.setDaemon(true);
    	    jarResourceDaemon.start();
	    }
	}
	
	public static Resource createResource(File... files) {
		if (files.length == 1) {
			File f = files[0];
			return createResource(f);
		} else if (files.length > 1) {
			Resource[] resources = new Resource[files.length];
			int i = 0;
			for (File f : files) {
				resources[i] = createResource(f);
				i++;
			}
			return new MultiResource(resources);
		}
		return null;
	}

	public static Resource createResource(File file) {
		if (file.isFile()) {
			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(file);
			} catch (Exception ex) {
				// ignore this.
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Could not create a ZipFile instance for file: " + file.getAbsolutePath() + ". The file may not be a zip/jar archive.", ex);
				}
			}
			if (zipFile != null) {
				return new JarResource(file, jarResourceDaemon);
			} else {
				return new FileResource(file);
			}
		} else if (file.isDirectory()) {
			return new DirResource(file);
		}
		throw new RuntimeException(new FileNotFoundException("Cannot find file " + file.getAbsolutePath()));
	}

	public abstract List<URL> getEntry(String path) throws Exception;

	public abstract URL getEntryRoot(String path) throws Exception;

	public abstract byte[] getEntryBytes(String path) throws Exception;

	public abstract Iterator<String> getAllEntries() throws Exception;
	
	public abstract Iterator<String> getAllEntriesIncludingFolders() throws Exception;

	protected byte[] readBytes(InputStream in) throws IOException {
		BufferedInputStream bin = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		try {
			bin = new BufferedInputStream(in);
			int read = 0;
			while ((read = bin.read(buffer)) > -1) {
				baos.write(buffer, 0, read);
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return baos.toByteArray();
	}

}
