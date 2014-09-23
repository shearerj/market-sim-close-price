package utils;

import java.io.Serializable;
import java.util.Iterator;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;

public interface SparseIterator<E> extends Iterator<E> {

	public boolean hasNextSparse();
	
	public SparseElement<E> nextSparse();
	
	/** A "sparse" element with an attached index */
	public static class SparseElement<E> implements Comparable<SparseElement<E>>, Serializable {
		private static final long serialVersionUID = -7342051252514803466L;

		public static <E> SparseElement<E> create(long index, E element) {
			return new SparseElement<E>(index, element);
		}
		
		public long index;
		public E element;
		
		private SparseElement(long index, E element) {
			this.index = index;
			this.element = element;
		}
		
		@Override
		public int hashCode() {
			return Objects.hashCode(index, element);
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof SparseElement))
				return false;
			SparseElement<?> se = (SparseElement<?>) o;
			return Objects.equal(this.index, se.index) && Objects.equal(this.element, se.element);
		}

		@Override
		public int compareTo(SparseElement<E> other) {
			return Longs.compare(this.index, other.index);
		}
		
		@Override
		public String toString() {
			return "(" + index + ": " + element + ")";
		}
	}
	
}
