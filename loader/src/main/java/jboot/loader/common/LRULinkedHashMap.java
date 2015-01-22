package jboot.loader.boot.common;

import java.util.LinkedHashMap;

public class LRULinkedHashMap<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = 4772762202630915656L;

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private int maxSize;

	public LRULinkedHashMap(int initialCapacity) {
		super(initialCapacity, DEFAULT_LOAD_FACTOR, true); // LRU Map.
		this.maxSize = initialCapacity;
	}

	public LRULinkedHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor, true); // LRU Map.
		this.maxSize = initialCapacity;
	}

	@Override
	public boolean containsKey(Object key) {
		if (super.containsKey(key)) {
			get(key); // In order to generate an access to the item and make it MRU.
			return true;
		}
		return false;
	}

	@Override
	protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
		return this.size() > maxSize;
	}
}
