package fourheap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

public class BinaryHeap<E> implements Queue<E> {

	protected final ArrayList<E> elements;
	protected final Map<E, Integer> mapping;
	protected final Comparator<E> comp;

	public BinaryHeap() {
		this(CompareUtils.<E> naturalOrder());
	}

	public BinaryHeap(Comparator<E> comp) {
		this(8, comp);
	}

	public BinaryHeap(int initialSize, Comparator<E> comp) {
		elements = new ArrayList<E>(initialSize);
		mapping = new HashMap<E, Integer>(initialSize);
		this.comp = comp;
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return mapping.containsKey(o);
	}

	@Override
	public Iterator<E> iterator() {
		return Collections.unmodifiableList(elements).iterator();
	}

	@Override
	public Object[] toArray() {
		return elements.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return elements.toArray(a);
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
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o)) return false;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		for (E elem : c) {
			int pos = size();
			elements.add(elem);
			mapping.put(elem, pos);
		}
		heapify();
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Make more efficient by removing everything and doing a heapify?
		boolean modified = false;
		for (Object o : c)
			modified |= remove(o);
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Make more efficient by retaining all and doing a heapify?
		Set<E> toRemove = new HashSet<E>(elements);
		toRemove.removeAll(c);
		return removeAll(toRemove);
	}

	@Override
	public void clear() {
		elements.clear();
		mapping.clear();
	}

	@Override
	public boolean add(E e) {
		int pos = size();
		elements.add(e);
		mapping.put(e, pos);
		shiftUp(pos);
		return true;
	}

	@Override
	public boolean offer(E e) {
		return add(e);
	}

	@Override
	public E remove() {
		if (isEmpty()) throw new NoSuchElementException();
		return poll();
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
	public E element() {
		if (isEmpty()) throw new NoSuchElementException();
		return elements.get(0);
	}

	@Override
	public E peek() {
		if (isEmpty()) return null;
		return elements.get(0);
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

	@Override
	public String toString() {
		return elements.toString();
	}

}
