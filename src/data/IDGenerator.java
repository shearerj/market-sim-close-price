package data;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

// TODO Test
public class IDGenerator implements Iterable<Integer> {

	protected final int step;

	public IDGenerator(int step) {
		if (step == 0)
			throw new IllegalArgumentException(
					"Can't create an IDGenerator with step 0");
		this.step = step;
	}

	@Override
	public Iterator<Integer> iterator() {
		return listIterator(0);
	}

	public ListIterator<Integer> listIterator(int initialIndex) {
		return new IDGeneratorIterator(step, initialIndex);
	}

	protected static class IDGeneratorIterator implements ListIterator<Integer> {

		protected final int step;
		protected int value;

		protected IDGeneratorIterator(int step, int initialIndex) {
			this.step = step;
			value = step*initialIndex;
		}

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public Integer next() {
			value += step;
			return value;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"Can't remove elements from an IDGenerator");
		}

		@Override
		public void add(Integer arg0) {
			throw new UnsupportedOperationException(
					"Can't add elements to an IDGenerator");

		}

		@Override
		public boolean hasPrevious() {
			return step > 0 ? value >= step : value <= step;
		}

		@Override
		public int nextIndex() {
			return value / step;
		}

		@Override
		public Integer previous() {
			if (!hasPrevious())
				throw new NoSuchElementException("IDGenerator has no previous element");
			int prev = value;
			value -= step;
			return prev;
		}

		@Override
		public int previousIndex() {
			return nextIndex() - 1;
		}

		@Override
		public void set(Integer arg0) {
			throw new UnsupportedOperationException(
					"Can't set elements in an IDGenerator");
		}

	}

}
