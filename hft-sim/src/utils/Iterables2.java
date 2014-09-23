package utils;

import java.util.Iterator;
import java.util.Random;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import utils.SparseIterator.SparseElement;

public class Iterables2 {
	
	private static final Random rand = new Random();

	public static <E> SparseIterable<E> toSparse(final Iterable<SparseElement<E>> iter) {
		return new SparseIterable<E>() {
			@Override
			public SparseIterator<E> iterator() {
				return Iterators2.fromSparse(iter.iterator());
			}

			@Override
			public String toString() {
				return Iterables.toString(iter);
			}
		};
	}
	
	public static <E> Iterable<SparseElement<E>> fromSparse(final SparseIterable<E> iter) {
		return new Iterable<SparseElement<E>>() {
			@Override
			public Iterator<SparseElement<E>> iterator() {
				return Iterators2.toSparse(iter.iterator());
			}
			
			@Override
			public String toString() {
				return Iterators.toString(iterator());
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
	
	public static Iterable<Integer> counter() {
		return new Iterable<Integer>() {
			@Override public Iterator<Integer> iterator() {
				return Iterators2.counter();
			}
		};
	}
	
	public static Iterable<Double> exponentials(final double rate, final Random rand) {
		return new Iterable<Double>() {
			@Override public Iterator<Double> iterator() {
				return Iterators2.exponentials(rate, rand);
			}
		};
	}
	
	public static <E> Iterable<E> shuffle(final Iterable<E> iter, Random rand) {
		final long seed = rand.nextLong();
		return new Iterable<E>() {
			@Override
			public Iterator<E> iterator() {
				return Iterators2.shuffle(iter.iterator(), new Random(seed));
			}
		};
	}
	
	public static <E> Iterable<E> shuffle(final Iterable<E> iter) {
		final long seed = rand.nextLong();
		return new Iterable<E>() {
			@Override
			public Iterator<E> iterator() {
				return Iterators2.shuffle(iter.iterator(), new Random(seed));
			}
		};
	}
	
}
