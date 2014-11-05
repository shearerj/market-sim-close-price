package utils;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import utils.SparseIterator.SparseElement;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.IntMath;


public class Iterators2 {
	
	private static final Rand rand = Rand.create();

	public static <E> SparseIterator<E> fromSparse(final Iterator<SparseElement<E>> iter) {
		return new SparseIterator<E>() {
			PeekingIterator<SparseElement<E>> backing = Iterators.peekingIterator(iter);
			long index = 0; // Index of the next returned element
			E currentElement = null;
			
			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public E next() {
				if (backing.hasNext() && backing.peek().index == index) {
					currentElement = backing.next().element;
				}
				index += 1;
				return currentElement;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Remove is not defined for a SparseIterator");
			}

			@Override
			public boolean hasNextSparse() {
				return backing.hasNext();
			}

			@Override
			public SparseElement<E> nextSparse() {
				SparseElement<E> next = backing.next();
				index = next.index + 1;
				currentElement = next.element;
				return next;
			}
			
		};
	}
	
	public static <E> Iterator<SparseElement<E>> toSparse(final SparseIterator<E> iter) {
		return new UnmodifiableIterator<SparseElement<E>>() {

			@Override
			public boolean hasNext() {
				return iter.hasNextSparse();
			}

			@Override
			public SparseElement<E> next() {
				return iter.nextSparse();
			}
		};
	}
	
	public static <E> Iterator<E> sample(final Iterator<E> iter, final int skip, final int offset) {
		checkArgument(skip >= 1, "Skip must be at least one");
		final int trueOffset = offset < 0 ? IntMath.mod(offset, skip) : offset;
		if (iter instanceof SparseIterator) {
			return new UnmodifiableIterator<E>() {

				PeekingIterator<SparseElement<E>> backing = Iterators.peekingIterator(toSparse((SparseIterator<E>) iter));
				long index = trueOffset; // Index of next returned element
				E currentElement = null;
				
				@Override
				public boolean hasNext() {
					return true;
				}

				@Override
				public E next() {
					while (backing.hasNext() && backing.peek().index <= index)
						currentElement = backing.next().element;
					index += skip;
					return currentElement;
				}				
			};
		} else {
			Iterators.advance(iter, trueOffset);
			return new UnmodifiableIterator<E>() {

				@Override
				public boolean hasNext() {
					return iter.hasNext();
				}

				@Override
				public E next() {
					E next = iter.next();
					Iterators.advance(iter, skip - 1);
					return next;
				}
			};
		}
	}
	
	public static Iterator<Integer> counter() {
		return new AbstractSequentialIterator<Integer>(1) {
			@Override protected Integer computeNext(Integer previous) { return previous + 1; }
		};
	}

	public static Iterator<Double> exponentials(final double rate, final Rand rand) {
		if (rate == 0)
			return ImmutableList.<Double> of().iterator();
		return new AbstractIterator<Double>() {
			@Override protected Double computeNext() {
				return rand.nextExponential(rate);
			}
		};
	}
	
	public static Iterator<String> lineIterator(Reader reader) {
		final BufferedReader lineReader = new BufferedReader(reader);
		try {
			return new UnmodifiableIterator<String>() {
				String line = lineReader.readLine();

				@Override
				public boolean hasNext() {
					return line != null;
				}

				@Override
				public String next() {
					String lastLine = line;
					try {
						line = lineReader.readLine();
					} catch (IOException e) {
						line = null;
					}
					return lastLine;
				}
			};
		} catch (IOException e) {
			return ImmutableList.<String> of().iterator();
		}
	}
	
	public static <E> Iterator<E> shuffle(Iterator<E> iter, Random rand) {
		List<E> copy = Lists.newArrayList(iter);
		Collections.shuffle(copy, rand);
		return Collections.unmodifiableList(copy).iterator();
	}
	
	public static <E> Iterator<E> shuffle(Iterator<E> iter) {
		return shuffle(iter, rand);
	}
	
}
