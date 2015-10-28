package edu.umich.srg.collect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * A RandomKeyedQueue is a
 * 
 * Move to supplier
 * 
 * Note that because of the dequeuing mechanism, if Activity A is supposed to happen before Activity
 * B, Activity A should queue up Activity B. Anything else may not guarantee that A always happens
 * before B.
 * 
 * @author ebrink
 */
public class RandomKeyedQueue<K, V> extends AbstractQueue<Entry<K, V>> {
  // FIXME, switch to UE Random queue / make this just take a supplier since those are easy now

  /*
   * Invariant that no event is ever empty at the end of execution.
   * 
   * In general the rule should be, if one activity comes logically after another activity it should
   * be scheduled by the activity that always proceeds it. Activities scheduled at the same time
   * (even infinitely fast) may occur in any order.
   */

  private NavigableMap<K, OrderedQueue<V>> queue;
  private int size;
  private Random rand;

  protected RandomKeyedQueue(Random seed, Comparator<? super K> comp) {
    this.queue = new TreeMap<>(comp);
    this.size = 0;
    this.rand = seed;
  }

  public static <K, V> RandomKeyedQueue<K, V> create(Random seed, Comparator<? super K> comp) {
    return new RandomKeyedQueue<>(seed, comp);
  }

  public static <K, V> RandomKeyedQueue<K, V> create(Comparator<? super K> comp) {
    return new RandomKeyedQueue<>(new Random(), comp);
  }

  public static <K extends Comparable<? super K>, V> RandomKeyedQueue<K, V> create(Random seed) {
    return new RandomKeyedQueue<>(seed, Comparator.naturalOrder());
  }

  public static <K extends Comparable<? super K>, V> RandomKeyedQueue<K, V> create() {
    return new RandomKeyedQueue<>(new Random(), Comparator.naturalOrder());
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return new EntryIterator();
  }

  @Override
  public void clear() {
    queue.clear();
    size = 0;
  }

  @Override
  public boolean offer(Entry<K, V> timedActivity) {
    return add(timedActivity.getKey(), timedActivity.getValue());
  }

  @Override
  public Entry<K, V> poll() {
    if (isEmpty())
      return null;
    Entry<K, OrderedQueue<V>> first = queue.firstEntry();
    V ret = first.getValue().poll();
    size--;
    if (first.getValue().isEmpty())
      queue.pollFirstEntry();
    return Maps.immutableEntry(first.getKey(), ret);
  }

  @Override
  public Entry<K, V> peek() {
    if (isEmpty())
      return null;
    Entry<K, OrderedQueue<V>> first = queue.firstEntry();
    return Maps.immutableEntry(first.getKey(), first.getValue().peek());
  }

  protected OrderedQueue<V> getOrCreate(K time) {
    OrderedQueue<V> q = queue.get(time);
    if (q == null) {
      /*
       * FIXME Do we want a constrained random queue, or a uniform random queue over deques. The
       * constrianed random queue is nice because all orderings are uniform, but it also means
       * scheduling more activities at the end makes your first activities more likely. A random
       * queue would give equal weight to ever group of actions at the same time. I think we want
       * the constrained one.
       * 
       * This can be thought of in the continuous time domain. The constrained random queue is like
       * generating a random time for all of your activities and then sorting it and assigning the
       * times of your activities that way. The random queue is like picking the time of your first
       * activity, then rejection sampling your second activity until it's after the first, then the
       * third until it's after the second and so on. Neither seems great...
       */
      q = new UPOrderedRandomQueue<>(rand);
      queue.put(time, q);
    }
    return q;
  }

  public boolean add(K time, V activity) {
    size++;
    return getOrCreate(time).add(activity);
  }

  public boolean addAllOrdered(K time, Collection<? extends V> activities) {
    size += activities.size();
    return getOrCreate(time).addAllOrdered(activities);
  }

  public boolean addAll(ListMultimap<? extends K, ? extends V> activities) {
    for (Entry<? extends K, ? extends Collection<? extends V>> e : activities.asMap().entrySet())
      addAllOrdered(e.getKey(), e.getValue());
    return true;
  }

  protected class EntryIterator implements Iterator<Entry<K, V>> {
    K time = null;
    Iterator<Entry<K, OrderedQueue<V>>> mapIt = queue.entrySet().iterator();
    Iterator<V> it = ImmutableList.<V>of().iterator();

    @Override
    public boolean hasNext() {
      return it.hasNext() || mapIt.hasNext();
    }

    @Override
    public Entry<K, V> next() {
      if (!it.hasNext()) {
        Entry<K, OrderedQueue<V>> nextTime = mapIt.next();
        time = nextTime.getKey();
        it = nextTime.getValue().iterator();
      }
      return Maps.immutableEntry(time, it.next());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException(
          "Can't arbitrarily remove elements from an event queue");
    }

  }

}
