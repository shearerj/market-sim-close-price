package utils;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.math.IntMath;

import utils.SparseIterator.SparseElement;


public class Iterators2 {

	public static <E> SparseIterator<E> fromSparse(final Iterator<SparseElement<E>> iter) {
		return new SparseIterator<E>() {

			PeekingIterator<SparseElement<E>> backing = Iterators.peekingIterator(iter);
			long index = 0; // Index of next returned element
			E currentElement = null;
			
			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public E next() {
				while (backing.hasNext() && backing.peek().index <= index)
					currentElement = backing.next().element;
				index += 1;
				return currentElement;
			}

			@Override
			public void remove() {
				// XXX Unsure what this would do
				throw new UnsupportedOperationException("Remove is not defined for an IndexedIterator");
			}

			@Override
			public boolean hasNextSparse() {
				return backing.hasNext();
			}

			@Override
			public SparseElement<E> nextSparse() {
				SparseElement<E> next = backing.next();
				index = next.index;
				currentElement = next.element;
				return next;
			}
			
		};
	}
	
	public static <E> Iterator<SparseElement<E>> toSparse(final SparseIterator<E> iter) {
		return new Iterator<SparseElement<E>>() {

			@Override
			public boolean hasNext() {
				return iter.hasNextSparse();
			}

			@Override
			public SparseElement<E> next() {
				return iter.nextSparse();
			}

			@Override
			public void remove() {
				// XXX Also unsure what this would do
				throw new UnsupportedOperationException("Remove not supported");
			}
			
		};
	}
	
	public static <E> Iterator<E> sample(final Iterator<E> iter, final int skip, final int offset) {
		checkArgument(skip >= 1, "Skip must be at least one");
		final int trueOffset = offset < 0 ? IntMath.mod(offset, skip) : offset;
		if (iter instanceof SparseIterator) {
			return new Iterator<E>() {

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

				@Override
				public void remove() {
					// XXX Unsure what this would do
					throw new UnsupportedOperationException("Remove is not defined for an IndexedIterator");
				}
				
			};
		} else {
			Iterators.advance(iter, trueOffset);
			return new Iterator<E>() {

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

				@Override
				public void remove() {
					// XXX Implementing the proper remove would be difficult, and doesn't really make sense.
					throw new UnsupportedOperationException("Remove not supported");
				}
				
			};
		}
	}
	
}
