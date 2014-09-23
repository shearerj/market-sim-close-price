package utils;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;

/**
 * The EventQueue is a a queue of upcoming events which are composed of
 * activities to be executed. The actual events are abstracted in this
 * implementation, so this functions as a queue of activities. The main methods
 * that should be used are "add," which adds a single activity to the queue,
 * "addAll," which adds a collection of activities to the queue, and "remove,"
 * which removes the activity at the head of the queue. Events that are added at
 * the same TimeStamp will be dequeued in a uniform random order. To make an
 * event occur instantaneously give it a time of TimeStamp.IMMEDIATE;
 * 
 * Note that because of the dequeuing mechanism, if Activity A is supposed to
 * happen after Activity B, Activity A should queue up Activity B. Anything else
 * may not guarantee that A always happens before B.
 * 
 * @author ebrink
 */
public class RandomKeyedQueue<T, A> extends AbstractQueue<Entry<T, A>> {
	
	/*
	 * Invariant that no event is ever empty at the end of execution.
	 * 
	 * In general the rule should be, if one activity comes logically after
	 * another activity it should be scheduled by the activity that always
	 * proceeds it. Activities scheduled at the same time (even infinitely fast)
	 * may occur in any order.
	 */
	
	private NavigableMap<T, Queue<A>> queue;
	private int size;
	private Random rand;

	protected RandomKeyedQueue(Random seed, Comparator<? super T> comp) {
		this.queue = Maps.newTreeMap(comp);
		this.size = 0;
		this.rand = seed;
	}

	public static <T extends Comparable<T>, A> RandomKeyedQueue<T, A> create() {
		return create(new Random());
	}
	
	public static <T extends Comparable<T>, A> RandomKeyedQueue<T, A> create(Random seed) {
		return create(seed, Ordering.natural());
	}
	
	public static <T, A> RandomKeyedQueue<T, A> create(Random seed, Comparator<? super T> comp) {
		return new RandomKeyedQueue<T, A>(seed, comp);
	}
	
	Queue<A> createQueue() {
		return new Event(rand);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Iterator<Entry<T, A>> iterator() {
		return new EntryIterator();
	}

	@Override
	public void clear() {
		queue.clear();
		size = 0;
	}

	@Override
	public boolean offer(Entry<T, A> timedActivity) {
		return add(timedActivity.getKey(), timedActivity.getValue());
	}

	@Override
	public Entry<T, A> poll() {
		if (isEmpty())
			return null;
		Entry<T, Queue<A>> first = queue.firstEntry();
		A ret = first.getValue().poll();
		size--;
		if (first.getValue().isEmpty())
			queue.pollFirstEntry();
		return Maps.immutableEntry(first.getKey(), ret);
	}

	@Override
	public Entry<T, A> peek() {
		if (isEmpty())
			return null;
		Entry<T, Queue<A>> first = queue.firstEntry();
		return Maps.immutableEntry(first.getKey(), first.getValue().peek());
	}
	
	Queue<A> getOrCreate(T time) {
		Queue<A> q = queue.get(time);
		if (q == null) {
			q = createQueue();
			queue.put(time, q);
		}
		return q;
	}
	
	public boolean add(T time, A activity) {
		size++;
		return getOrCreate(time).add(activity);
	}
	
	public boolean addAll(T time, Collection<? extends A> activities) {
		size += activities.size();
		return getOrCreate(time).addAll(activities);
	}

	public boolean addAll(ListMultimap<? extends T, ? extends A> activities) {
		for (Entry<? extends T, ? extends Collection<? extends A>> e : activities.asMap().entrySet())
			addAll(e.getKey(), e.getValue());
		return true;
	}
	
	class EntryIterator implements Iterator<Entry<T, A>> {
		T time = null;
		Iterator<Entry<T, Queue<A>>> mapIt = queue.entrySet().iterator();
		Iterator<A> it = ImmutableList.<A> of().iterator();
		
		@Override
		public boolean hasNext() {
			return it.hasNext() || mapIt.hasNext();
		}

		@Override
		public Entry<T, A> next() {
			if (!it.hasNext()) {
				Entry<T, Queue<A>> nextTime = mapIt.next();
				time = nextTime.getKey();
				it = nextTime.getValue().iterator();
			}
			return Maps.immutableEntry(time, it.next());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Can't arbitrarily remove elements from an event queue");
		}
		
	}
	
	class Event extends AbstractQueue<A> {
		protected final Queue<Queue<A>> backedQueue;
		protected int size;

		public Event(Random seed) {
			size = 0;
			backedQueue = RandomQueue.create(seed);
		}

		@Override
		public boolean offer(A activity) {
			return addAll(ImmutableList.of(activity));
		}

		@Override
		public boolean addAll(Collection<? extends A> collection) {
			size += collection.size();
			return backedQueue.add(Queues.newArrayDeque(collection));
		}

		@Override
		public A poll() {
			if (backedQueue.isEmpty())
				return null;
			Queue<A> seq = backedQueue.poll();
			A ret = seq.poll();
			size--;
			if (!seq.isEmpty())
				backedQueue.offer(seq);
			return ret;
		}

		@Override
		public A peek() {
			if (backedQueue.isEmpty())
				return null;
			return backedQueue.peek().peek();
		}

		@Override
		public Iterator<A> iterator() {
			return Iterators.unmodifiableIterator(Iterables.concat(backedQueue).iterator());
		}

		@Override
		public int size() {
			return size;
		}

	}
	
}
