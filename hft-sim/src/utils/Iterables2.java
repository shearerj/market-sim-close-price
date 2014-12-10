package utils;

import java.util.Iterator;
import java.util.Random;

public class Iterables2 {
	
	private static final Random rand = new Random();
	
	public static Iterable<Integer> counter() {
		return new Iterable<Integer>() {
			@Override public Iterator<Integer> iterator() {
				return Iterators2.counter();
			}
		};
	}
	
	public static Iterable<Double> exponentials(final double rate, final Rand rand) {
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
