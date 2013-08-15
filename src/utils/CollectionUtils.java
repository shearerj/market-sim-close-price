package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

public class CollectionUtils {
	
	public static <E extends Comparable<? super E>> List<E> mergeSortedLists(Collection<List<? extends E>> lists) {
		return mergeSortedLists(lists, Compare.<E> naturalOrder());
	}
	
	public static <E> List<E> mergeSortedLists(Collection<? extends List<? extends E>> lists, Comparator<? super E> comp) {
		PriorityQueue<CompIterator<E>> queue = new PriorityQueue<CompIterator<E>>();
		for (List<? extends E> list : lists)
			if (!list.isEmpty())
				queue.add(new CompIterator<E>(list.iterator(), comp));
		
		List<E> merged = new ArrayList<E>();
		while (!queue.isEmpty()) {
			CompIterator<E> next = queue.remove();
			merged.add(next.next());
			if (next.hasNext())
				queue.add(next);
		}
		return merged;
	}
	
	public static <E> Collection<E> concat(Collection<? extends E>... collections) {
		return concat(Arrays.asList(collections));
	}
	
	public static <E> Collection<E> concat(List<? extends Collection<? extends E>> collections) {
		Collection<E> concated = new ArrayList<E>();
		for (Collection<? extends E> coll : collections)
			concated.addAll(coll);
		return concated;
	}

	public static <E> Iterable<E> toIterable(final Iterator<E> it) {
		return new Iterable<E>() {
			public Iterator<E> iterator() {
				return it;
			}
		};
	}

	public static <E> Iterable<E> toIterable(final Iterator<E> it, final int num) {
		return new Iterable<E>() {
			public Iterator<E> iterator() {
				return new LimitedIterator<E>(it, num);
			}
		};
	}

	public static <E> Iterator<E> emptyIterator() {
		return new EmptyIterator<E>();
	}
	
	private static class EmptyIterator<E> implements Iterator<E> {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public E next() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}
	
	private static class LimitedIterator<E> implements Iterator<E> {

		Iterator<E> it;
		int num;
		
		public LimitedIterator(Iterator<E> it, int num) {
			this.it = it;
			this.num = num;
		}
		
		@Override
		public boolean hasNext() {
			return num > 0;
		}

		@Override
		public E next() {
			if (!hasNext()) throw new NoSuchElementException();
			num--;
			return it.next();
		}

		@Override
		public void remove() {
			it.remove();
		}

	}
	
	private static class CompIterator<E> implements Iterator<E>, Comparable<CompIterator<E>> {

		E peekElem;
		Iterator<? extends E> it;
		Comparator<? super E> comp;
		
		public CompIterator(Iterator<? extends E> it, Comparator<? super E> comp) {
			this.it = it;
			this.comp = comp;
			if (it.hasNext()) peekElem = it.next();
			else peekElem = null;
		}
		
		@Override
		public boolean hasNext() {
			return peekElem != null;
		}

		@Override
		public E next() {
			E ret = peekElem;
			if (it.hasNext()) peekElem = it.next();
			else peekElem = null;
			return ret;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int compareTo(CompIterator<E> o) {
			if (peekElem == null) return 1;
			else return comp.compare(peekElem, o.peekElem);
		}
		
	}
	
}
