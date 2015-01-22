package jboot.loader.boot.node.resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class MultiResource extends Resource {

	private List<Resource> resources;

	public MultiResource(Resource... resources) {
		this.resources = Arrays.asList(resources);
	}

	public List<Resource> getResources() {
		return Collections.unmodifiableList(resources);
	}

	@Override
	public Iterator<String> getAllEntries() throws Exception {
		List<Iterator<String>> iterators = new ArrayList<Iterator<String>>(resources.size());
		for (Resource res : resources) {
			iterators.add(res.getAllEntries());
		}
		return new MultiIterator(iterators);
	}
	
	public Iterator<String> getAllEntriesIncludingFolders() throws Exception {
		List<Iterator<String>> iterators = new ArrayList<Iterator<String>>(resources.size());
		for (Resource res : resources) {
			iterators.add(res.getAllEntriesIncludingFolders());
		}
		return new MultiIterator(iterators);
	}

	@Override
	public List<URL> getEntry(String path) throws Exception {
		List<URL> entries = null;
		for (Resource res : resources) {
			List<URL> urls = res.getEntry(path);
			if (urls != null) {
				if (entries == null) {
					entries = new ArrayList<URL>();
				}
				entries.addAll(urls);
			}
		}
		return entries;
	}

	@Override
	public byte[] getEntryBytes(String path) throws Exception {
		for (Resource res : resources) {
			byte[] bytes = res.getEntryBytes(path);
			if (bytes != null) {
				return bytes;
			}
		}
		return null;
	}

	@Override
	public URL getEntryRoot(String path) throws Exception {
		for (Resource res : resources) {
			URL url = res.getEntryRoot(path);
			if (url != null) {
				return url;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		String result = this.getClass().getSimpleName() + ": ";
		for (Resource res : resources) {
			result += "[";
			result += res.toString();
			result += "]";
		}
		return result;
	}

	public static class MultiIterator implements Iterator<String> {
		public LinkedList<Iterator<String>> iterators;

		public MultiIterator(Collection<Iterator<String>> iterators) {
			this.iterators = new LinkedList<Iterator<String>>(iterators);
		}

		@Override
		public boolean hasNext() {
			while (iterators.size() > 0) {
				if (iterators.getFirst().hasNext()) {
					return true;
				} else {
					iterators.removeFirst();
				}
			}
			return false;
		}

		@Override
		public String next() {
			if (hasNext()) {
				return iterators.getFirst().next();
			}
			throw new NoSuchElementException("No more entries in resource.");
		}

		@Override
		public void remove() {
		}
	}
}
