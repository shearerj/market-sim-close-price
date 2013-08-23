/*
 * Copyright 2003-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package fourheap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

/**
 * Adapted from Java PriorityQueue Source
 */
public class BinaryHeap<E> extends AbstractQueue<E> implements Serializable {
	private static final long serialVersionUID = 1999398475077245857L;

	protected final List<E> queue;
	protected final Ordering<? super E> ordering;
	protected final Map<E, Integer> locations;
	protected transient int modCount = 0; // For iterator concurrent modifications

	protected BinaryHeap(int initialCapacity, Comparator<? super E> comparator) {
		this.queue = Lists.newArrayListWithCapacity(initialCapacity);
		this.locations = Maps.newHashMapWithExpectedSize(initialCapacity);
		this.ordering = Ordering.from(comparator);
	}
	
	public static <E extends Comparable<? super E>> BinaryHeap<E> create() {
		return create(Ordering.natural());
	}
	
	public static <E> BinaryHeap<E> create(Comparator<? super E> comparator) {
		return create(8, comparator);
	}

	public static <E extends Comparable<? super E>> BinaryHeap<E> create(
			int initialCapactiy) {
		return create(initialCapactiy, Ordering.natural());
	}
	
	public static <E> BinaryHeap<E> create(int initialCapacity, Comparator<? super E> comparator) {
		return new BinaryHeap<E>(initialCapacity, comparator);
	}
	
	public static <E extends Comparable<? super E>> BinaryHeap<E> create(
			Iterable<? extends E> initialElements) {
		return create(initialElements, Ordering.natural());
	}
	
	public static <E> BinaryHeap<E> create(
			Iterable<? extends E> initialElements,
			Comparator<? super E> comparator) {
		
		BinaryHeap<E> queue = create(comparator);
		Iterables.addAll(queue.queue, initialElements);
		queue.heapify();
		return queue;
	}

	@Override
	public boolean offer(E e) {
		checkNotNull(e);
		modCount++;
		int i = size();
		queue.add(e);
		siftUp(i, e);
		return true;
	}

	@Override
	public E peek() {
		return Iterables.getFirst(queue, null);
	}

	@Override
	public boolean remove(Object o) {
		Integer loc = locations.remove(o);
		if (loc == null) return false;
		removeAt(loc);
		return true;
	}

	/**
	 * Version of remove using reference equality, not equals. Needed by iterator.remove.
	 */
	boolean removeEq(Object o) {
		int i = 0;
		for (E e : queue) {
			if (o == e) {
				removeAt(i);
				return true;
			}
			i++;
		}
		return false;
	}

	@Override
	public boolean contains(Object o) {
		return locations.containsKey(o);
	}

	@Override
	public Iterator<E> iterator() {
		return new HeapIterator();
	}

	@Override
	public int size() {
		return queue.size();
	}

	@Override
	public void clear() {
		modCount++;
		queue.clear();
		locations.clear();
	}

	@Override
	public E poll() {
		if (isEmpty()) return null;
		modCount++;
		E result = queue.get(0);
		locations.remove(result);
		int s = size() - 1;
		E x = queue.remove(s);
		if (s != 0) siftDown(0, x);
		return result;
	}

	/**
	 * Removes the ith element from queue.
	 * 
	 * Normally this method leaves the elements at up to i-1, inclusive, untouched. Under these
	 * circumstances, it returns null. Occasionally, in order to maintain the heap invariant, it
	 * must swap a later element of the list with one earlier than i. Under these circumstances,
	 * this method returns the element that was previously at the end of the list and is now at some
	 * position before i. This fact is used by iterator.remove so as to avoid missing traversing
	 * elements.
	 */
	protected E removeAt(int i) {
		assert i >= 0 && i < size();
		modCount++;
		int s = size() - 1;
		locations.remove(queue.get(i));
		if (s == i) { // removed last element
			queue.remove(s);
		} else {
			E moved = queue.remove(s);
			siftDown(i, moved);
			if (queue.get(i) == moved) {
				siftUp(i, moved);
				if (queue.get(i) != moved) return moved;
			}
		}
		return null;
	}

	/**
	 * Inserts item x at position k, maintaining heap invariant by promoting x up the tree until it
	 * is greater than or equal to its parent, or is the root.
	 * 
	 * To simplify and speed up coercions and comparisons. the Comparable and Comparator versions
	 * are separated into different methods that are otherwise identical. (Similarly for siftDown.)
	 */
	protected void siftUp(int k, E x) {
		while (k > 0) {
			int parent = (k - 1) / 2;
			E e = queue.get(parent);
			if (ordering.compare(x, e) >= 0) break;
			queue.set(k, e);
			locations.put(e, k);
			k = parent;
		}
		queue.set(k, x);
		locations.put(x, k);
	}

	/**
	 * Inserts item x at position k, maintaining heap invariant by demoting x down the tree
	 * repeatedly until it is less than or equal to its children or is a leaf.
	 */
	protected void siftDown(int k, E x) {
		int half = size() / 2;
		while (k < half) {
			int child = (k * 2) + 1;
			E c = queue.get(child);
			int right = child + 1;
			if (right < size() && ordering.compare( c, (E) queue.get(right)) > 0)
				c = queue.get(child = right);
			if (ordering.compare(x, c) <= 0) break;
			queue.set(k, c);
			locations.put(c, k);
			k = child;
		}
		queue.set(k, x);
		locations.put(x, k);
	}

	/**
	 * Establishes the heap invariant (described above) in the entire tree, assuming nothing about
	 * the order of the elements prior to the call.
	 */
	protected void heapify() {
		for (int i = size() / 2 - 1; i >= 0; i--)
			siftDown(i, queue.get(i));
	}

	public Ordering<? super E> ordering() {
		return ordering;
	}

	protected final class HeapIterator implements Iterator<E> {
		/**
		 * Index (into queue array) of element to be returned by subsequent call to next.
		 */
		private int cursor = 0;
	
		/**
		 * Index of element returned by most recent call to next, unless that element came from the
		 * forgetMeNot list. Set to -1 if element is deleted by a call to remove.
		 */
		private int lastRet = -1;
	
		/**
		 * A queue of elements that were moved from the unvisited portion of the heap into the
		 * visited portion as a result of "unlucky" element removals during the iteration. (Unlucky
		 * element removals are those that require a siftup instead of a siftdown.) We must visit
		 * all of the elements in this list to complete the iteration. We do this after we've
		 * completed the "normal" iteration.
		 * 
		 * We expect that most iterations, even those involving removals, will not need to store
		 * elements in this field.
		 */
		protected final ArrayDeque<E> forgetMeNot = new ArrayDeque<E>();
	
		/**
		 * Element returned by the most recent call to next iff that element was drawn from the
		 * forgetMeNot list.
		 */
		private E lastRetElt = null;
	
		/**
		 * The modCount value that the iterator believes that the backing Queue should have. If this
		 * expectation is violated, the iterator has detected concurrent modification.
		 */
		private int expectedModCount = modCount;

		public boolean hasNext() {
			return cursor < size() || !forgetMeNot.isEmpty();
		}
	
		public E next() {
			if (expectedModCount != modCount)
				throw new ConcurrentModificationException();
			if (cursor < size()) return queue.get(lastRet = cursor++);
			lastRet = -1;
			return lastRetElt = forgetMeNot.poll();
		}
	
		public void remove() {
			if (expectedModCount != modCount) throw new ConcurrentModificationException();
			if (lastRet >= 0) {
				E moved = BinaryHeap.this.removeAt(lastRet);
				lastRet = -1;
				if (moved == null)
					cursor--;
				else
					forgetMeNot.add(moved);
			} else {
				checkState(lastRetElt != null);
				BinaryHeap.this.removeEq(lastRetElt);
			}
			expectedModCount = modCount;
		}
	}

}
