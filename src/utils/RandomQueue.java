/**
 * 
 */
package utils;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Randomly ordered queue. Elements will come out in a random order independent
 * of when they were inserted. Given a collection of elements in the queue at a
 * given time. The probability that any specific element will be removed next is
 * uniform.
 * 
 * @author ebrink
 * 
 */
public class RandomQueue<E> extends AbstractQueue<E> {

	protected Random rand;
	protected List<E> elements;

	public RandomQueue() {
		this(new Random());
	}

	public RandomQueue(Random seed) {
		elements = Lists.newArrayList();
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
	public void clear() {
		elements.clear();
	}

	@Override
	public Iterator<E> iterator() {
		return elements.iterator();
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public boolean offer(E e) {
		elements.add(e);
		int swap = rand.nextInt(size());
		elements.set(size() - 1, elements.get(swap));
		elements.set(swap, e);
		return true;
	}

	@Override
	public E peek() {
		return Iterables.getLast(elements, null);
	}

	@Override
	public E poll() {
		if (isEmpty())
			return null;
		return elements.remove(size() - 1);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj.getClass().equals(getClass()))) return false;
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
