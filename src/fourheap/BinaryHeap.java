package fourheap;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

// TODO Implement iterator remove
// TODO change workings to not use swap, but instead only update when necessary. Basically, move swap into shiftUp and ShiftDown
public class BinaryHeap<E> extends AbstractQueue<E> implements Serializable {

	private static final long serialVersionUID = 6048249591836891615L;
	
	protected final List<E> elements;
	protected final Map<E, Integer> mapping;
	protected final Comparator<E> comp;

	protected BinaryHeap(int initialSize, Comparator<E> comp) {
		elements = Lists.newArrayListWithExpectedSize(initialSize);
		mapping = Maps.newHashMapWithExpectedSize(initialSize);
		this.comp = comp;
	}
	
	public static <E extends Comparable<? super E>> BinaryHeap<E> create() {
		return create(8);
	}
	
	public static <E extends Comparable<? super E>> BinaryHeap<E> create(int initialCapactiy) {
		return create(initialCapactiy, Ordering.<E> natural());
	}
	
	public static <E> BinaryHeap<E> create(Comparator<E> comp) {
		return create(8, comp);
	}
	
	public static <E> BinaryHeap<E> create(int initialCapacity, Comparator<E> comp) {
		return new BinaryHeap<E>(initialCapacity, comp);
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public boolean contains(Object o) {
		return mapping.containsKey(o);
	}

	@Override
	public Iterator<E> iterator() {
		return Iterators.unmodifiableIterator(elements.iterator());
	}

	@Override
	public boolean remove(Object o) {
		Integer index = mapping.get(o);
		if (index == null) return false;
		swap(index, size() - 1);
		E head = elements.remove(size() - 1);
		mapping.remove(head);
		if (index < size()) if (!shiftUp(index)) shiftDown(index);
		return true;
	}

	@Override
	public void clear() {
		elements.clear();
		mapping.clear();
	}

	@Override
	public boolean offer(E e) {
		int pos = size();
		elements.add(e);
		mapping.put(e, pos);
		shiftUp(pos);
		return true;
	}

	@Override
	public E poll() {
		if (isEmpty()) return null;
		swap(0, size() - 1);
		E head = elements.remove(size() - 1);
		mapping.remove(head);
		shiftDown(0);
		return head;
	}

	@Override
	public E peek() {
		return Iterables.getFirst(elements, null);
	}
	
	public Comparator<E> comparator() {
		return comp;
	}

	/**
	 * Swaps elements in position i and j
	 */
	protected void swap(int i, int j) {
		assert (i < elements.size() && i >= 0 && j < elements.size() && j > 0);
		E ei = elements.get(i);
		E ej = elements.get(j);

		elements.set(i, ej);
		elements.set(j, ei);
		mapping.put(ej, i);
		mapping.put(ei, j);
	}

	protected void shiftDown(int pos) {
		while (true) {
			int child = pos * 2 + 1;
			if (child >= size()) break;
			E c = elements.get(child);
			int right = child + 1;
			if (right < size() && comp.compare(c, elements.get(right)) > 0)
				c = elements.get(child = right);
			if (comp.compare(elements.get(pos), c) <= 0) break;
			swap(pos, child);
			pos = child;
		}
	}

	protected boolean shiftUp(int pos) {
		boolean modified = false;
		while (pos > 0) {
			int parent = (pos - 1) / 2;
			E e = elements.get(parent);
			if (comp.compare(elements.get(pos), e) >= 0) break;
			modified = true;
			swap(pos, parent);
			pos = parent;
		}
		return modified;
	}

	protected void heapify() {
		for (int pos = (size() / 2) - 1; pos >= 0; pos--)
			shiftDown(pos);
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) return false;
		BinaryHeap<?> other = (BinaryHeap<?>) obj;
		return elements.equals(other.elements);
	}

}
