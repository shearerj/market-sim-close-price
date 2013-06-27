package utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CollectionUtils {

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
	
}
