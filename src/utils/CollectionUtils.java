package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CollectionUtils {
	
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
	
}
