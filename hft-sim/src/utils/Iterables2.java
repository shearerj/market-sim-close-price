package utils;

import java.util.Iterator;

import utils.SparseIterator.SparseElement;

public class Iterables2 {

	public static <E> SparseIterable<E> fromSparse(final Iterable<SparseElement<E>> iter) {
		return new SparseIterable<E>() {
			@Override
			public SparseIterator<E> iterator() {
				return Iterators2.fromSparse(iter.iterator());
			}
		};
	}
	
	public static <E> Iterable<SparseElement<E>> toSparse(final SparseIterable<E> iter) {
		return new Iterable<SparseElement<E>>() {
			@Override
			public Iterator<SparseElement<E>> iterator() {
				return Iterators2.toSparse(iter.iterator());
			}
		};
	}
	
	public static <E> Iterable<E> sample(final Iterable<E> iter, final int skip, final int offset) {
		return new Iterable<E>() {
			@Override
			public Iterator<E> iterator() {
				return Iterators2.sample(iter.iterator(), skip, offset);
			}
		};
	}
	
}
