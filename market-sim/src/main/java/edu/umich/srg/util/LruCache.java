package edu.umich.srg.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class LruCache<K, V> extends LinkedHashMap<K, V> {

  private static final long serialVersionUID = -2601892331869611178L;
  private final int maxSize;

  public LruCache(int maxSize, int initialCapacity, float loadFactor) {
    super(Math.min(maxSize, initialCapacity), loadFactor, true);
    this.maxSize = maxSize;
  }

  public LruCache(int maxSize) {
    this(maxSize, 16, 0.75f);
  }

  @Override
  protected boolean removeEldestEntry(Entry<K, V> eldest) {
    return size() > maxSize;
  }

}
