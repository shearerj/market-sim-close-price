package edu.umich.srg.collect;

import java.util.Iterator;
import java.util.Queue;
import java.util.function.Function;

import com.google.common.collect.PeekingIterator;

public final class Iters {
	
	/**
	 * Repeats every element in an iterable a given number of times.
	 * @param iterator The iterator to repeat every element of.
	 * @param num The number of times to repeat elements
	 * @return
	 */
	public static <T> Iterator<T> repeat(Iterator<T> iterator, long num) {
		PeekingIterator<T> peekable = com.google.common.collect.Iterators.peekingIterator(iterator);
		return new Iterator<T>() {

			long numLeft = num;
			
			@Override
			public boolean hasNext() {
				return peekable.hasNext();
			}

			@Override
			public T next() {
				T item = peekable.peek();
				if (--numLeft == 0) {
					peekable.next();
					numLeft = num;
				}
				return item;
			}
			
		};
	}
	
	public static <T> Iterator<T> consumeQueue(Queue<T> queue) {
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {
				return !queue.isEmpty();
			}

			@Override
			public T next() {
				return queue.remove();
			}
			
		};
	}
	
	// FIXME Remove in favor of converting to a stream
	public static <I, R> Iterator<R> map(Iterator<I> iterator, Function<I, R> mappingFunction) {
		return new Iterator<R>() {
			
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public R next() {
				return mappingFunction.apply(iterator.next());
			}
			
		};
	}
	
	public static final <T> Iterator<Enumerated<T>> enumerate(Iterator<T> iterator) {
		return new Iterator<Enumerated<T>>() {
			private long index = 0;

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Enumerated<T> next() {
				return new Enumerated<T>(index++, iterator.next());
			}
			
		};
	}
	
	public static final class Enumerated<T> {
		public final long index;
		public final T obj;
		private Enumerated(long index, T obj) {
			this.index = index;
			this.obj = obj;
		}
	}
	
	// TODO There has to be a better way
	/** This is a wrapper iterator that will print any exception stack traces that may be gobbled up by a thread pool */
	public static final <T> Iterator<T> printExceptions(Iterator<T> iterator) {
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public T next() {
				try {
					return iterator.next();
				} catch (Exception e) {
					e.printStackTrace();
					throw e;
				}
			}
		};
	}
	
	private Iters() { };
	
}
