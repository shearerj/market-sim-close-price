package edu.umich.srg.collect;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableList;

public class Streams {

	/**
	 * Turn an iterator into a stream that is optionally parallel.
	 * 
	 * @param iterator
	 *            The iterator to turn into a stream.
	 * @param parallel
	 *            Whether to make the stream parallel. Note, unlike most streams
	 *            changing whether or not the stream is parallel after
	 *            construction will not have the desired efficiency benefits.
	 * @return A stream that is backed by the iterator and optionally supports
	 *         parallel computation.
	 */
	public static <T> Stream<T> stream(Iterator<T> iterator, boolean parallel) {
		if (parallel)
			return StreamSupport.stream(new ParallelIteratorSpliterator<>(iterator), true);
		else
			return stream(iterator);
	}
	
	public static <T> Stream<T> stream(Iterator<T> iterator) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}

	private static final class ParallelIteratorSpliterator<T> implements Spliterator<T> {

		private final Iterator<T> iterator;

		private ParallelIteratorSpliterator(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		public int characteristics() {
			return Spliterator.IMMUTABLE & Spliterator.SUBSIZED & Spliterator.ORDERED;
		}

		@Override
		public long estimateSize() {
			return Long.MAX_VALUE;
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			if (!iterator.hasNext())
				return false;
			action.accept(iterator.next());
			return true;
		}

		@Override
		public Spliterator<T> trySplit() {
			if (iterator.hasNext())
				return ImmutableList.of(iterator.next()).spliterator();
			else
				return null;
		}

	}

}
