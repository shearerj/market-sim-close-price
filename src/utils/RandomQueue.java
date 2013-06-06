/**
 * 
 */
package utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

/**
 * Randomly ordered queue. Elements will come out in a random order independent
 * of when they were inserted. Given a collection of elements in the queue at a
 * given time. The probability that any specific element will be removed next is
 * uniform.
 * 
 * @author ebrink
 * 
 */
public class RandomQueue<E> implements Queue<E> {

	protected Random rand;
	protected ArrayList<E> elements;

	public RandomQueue() {
		elements = new ArrayList<E>();
		rand = new Random();
	}

	public RandomQueue(Random seed) {
		elements = new ArrayList<E>();
		rand = seed;
	}

	public RandomQueue(Collection<? extends E> initialElements) {
		this();
		addAll(initialElements);
	}

	public RandomQueue(Collection<? extends E> initialElements, Random seed) {
		this(seed);
		addAll(initialElements);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (c.isEmpty())
			return false;
		for (E elem : c)
			add(elem);
		return true;
	}

	@Override
	public void clear() {
		elements.clear();
	}

	@Override
	public boolean contains(Object o) {
		return elements.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return elements.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return elements.iterator();
	}

	@Override
	public boolean remove(Object o) {
		return elements.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c)
			changed |= remove(o);
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return elements.retainAll(c);
	}

	@Override
	public int size() {
		return elements.size();
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
	public boolean add(E e) {
		elements.add(e);
		int swap = rand.nextInt(size());
		elements.set(size() - 1, elements.get(swap));
		elements.set(swap, e);
		return true;
	}

	@Override
	public E element() {
		if (isEmpty())
			throw new NoSuchElementException("RandomQueue is empty");
		return peek();
	}

	@Override
	public boolean offer(E e) {
		return add(e);
	}

	@Override
	public E peek() {
		if (isEmpty())
			return null;
		return elements.get(size() - 1);
	}

	@Override
	public E poll() {
		if (isEmpty())
			return null;
		return elements.remove(size() - 1);
	}

	@Override
	public E remove() {
		if (isEmpty())
			throw new NoSuchElementException("RandomQueue is empty");
		return poll();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof RandomQueue))
			return false;
		return elements.equals(((RandomQueue<?>) obj).elements);
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}

	@Override
	public String toString() {
		return elements.toString();
	}

}
