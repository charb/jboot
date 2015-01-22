package jboot.loader.boot.node.resource;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class DirResource extends Resource {

	private File file;

	public DirResource(File file) {
		if (!file.isDirectory()) {
			throw new IllegalArgumentException("The specified File instance does not represent a directory. " + file.getAbsolutePath());
		}
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	@Override
	public Iterator<String> getAllEntries() throws Exception {
		return new DirectoryEntriesIterator(file, false);
	}

	@Override
	public Iterator<String> getAllEntriesIncludingFolders() throws Exception {
		return new DirectoryEntriesIterator(file, true);
	}
	
	@Override
	public List<URL> getEntry(String path) throws Exception {
		List<URL> entries = null;
		URL url = findEntry(path);
		if (url != null) {
			entries = new ArrayList<URL>(1);
			entries.add(url);
		}
		return entries;
	}

	@Override
	public byte[] getEntryBytes(String path) throws Exception {
		URL url = findEntry(path);
		if (url != null) {
			InputStream in = url.openStream();
			return readBytes(in);
		}
		return null;
	}

	@Override
	public URL getEntryRoot(String path) throws Exception {
		URL url = findEntry(path);
		if (url != null) {
			return file.toURI().toURL();
		}
		return null;
	}

	protected URL findEntry(String path) throws MalformedURLException {
		path = path.replaceAll("\\\\", "/");
		Iterator<String> entryIter = new DirectoryEntriesIterator(file, false);
		while (entryIter.hasNext()) {
			String entry = entryIter.next();
			if (entry.equals(path)) {
				File f = new File(file, entry);
				if (f.exists()) {
					return f.toURI().toURL();
				} else {
					return null;
				}
			}	
		}
		return null;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + getFile().getAbsolutePath();
	}

	public static class DirectoryEntriesIterator implements Iterator<String> {
		private File root;
		private boolean includeFolders;
		private LinkedList<String> directories;
		private LinkedList<String> files;

		public DirectoryEntriesIterator(File root, boolean includeFolders) {
			this.root = root;
			this.includeFolders = includeFolders;
			directories = new LinkedList<String>();
			directories.add("");
			files = new LinkedList<String>();
		}

		@Override
		public boolean hasNext() {		
			if (files.size() > 0) {
				return true;
			}
			while (files.size() == 0) {
				if (directories.size() > 0) {
					String dir = directories.removeFirst();
					scanDirectory(dir);
				} else {
					return false;
				}
			}
			return true;
		}

		@Override
		public String next() {
			if (hasNext()) {
				return files.removeFirst();
			}			
			throw new NoSuchElementException("No more entries in resource.");
		}

		@Override
		public void remove() {
		}

		private void scanDirectory(String dirPath) {
			File dir = new File(root, dirPath);
			String[] entries = dir.list();
			if(includeFolders && !dirPath.isEmpty()) {
				files.add(dirPath);
			}
			for (String entry : entries) {
				File f = new File(dir, entry);
				if (f.isDirectory()) {
					directories.add(dirPath.isEmpty() ? entry : (dirPath + "/" + entry));
				} else {
					files.add(dirPath.isEmpty() ? entry : (dirPath + "/" + entry));
				}
			}
		}
	}
}
