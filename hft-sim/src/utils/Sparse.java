package utils;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import utils.Maths.Median;

import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.primitives.Ints;

/**
 * Class for operating on sparse sets of data points. Typically these are time
 * series objects.
 * 
 * In order to facilitate linear time in the the number of sparse data points,
 * and not in terms of the overall length of simulation, this interpolates all
 * sparse data points forward in time until a new sparse data point is found.
 * This assumption isn't always accurate, but it is necessary for compression.
 * 
 * A number of methods take a period and a length. The length is the total
 * number of data points to consider, inclusive of 0, but exclusive of "length".
 * Period is the period to sample points at. A period of 1 uses all of the
 * points, a period of 100 will sample one point for every 100 units. The point
 * sampled is the point at the end of the interval. So a period of 100 will
 * sample the point at 99, 199, 299, etc.
 * 
 * @author erik
 * 
 */
public final class Sparse {
	
	public static double median(Iterable<? extends SparseElement<? extends Number>> data, long length) {
		Median median = Median.of();
		for (DataRun e : compressData(data, 1, length))
			median.add(e.value, Ints.checkedCast(e.count)); // If this fails we need to make this take longs...
		return median.get();
	}
	
	public static double stddev(Iterable<? extends SparseElement<? extends Number>> data, long period, long length) {
		SummStats stddev = SummStats.on();
		for (DataRun e : compressData(data, period, length))
			stddev.addN(e.value, e.count);
		return stddev.stddev();
	}
	
	/** Calculates the standard deviation of the log ratio of successive data points - ignores nans */
	public static double logRatioStddev(Iterable<? extends SparseElement<? extends Number>> data, long period, long length) {
		SummStats stddev = SummStats.on();
		double prev = Double.NaN;
		for (DataRun e : compressData(data, period, length)) {
			double next = Math.log(e.value);
			double logr = next - prev;
			if (!Double.isNaN(logr))
				stddev.add(logr);
			prev = next;
			stddev.addN(0, e.count - 1);
		}
		return stddev.stddev();
	}
	
	/** Calcualte the rmsd between two functions */
	public static double rmsd(Iterable<? extends SparseElement<? extends Number>> first,
			Iterable<? extends SparseElement<? extends Number>> second, long period, long length) {
		SummStats mean = SummStats.on();
		for (DataPairRun e : compressData(first, second, period, length)) {
			if (!Double.isNaN(e.one) && !Double.isNaN(e.two)) // Should we do this?
				mean.addN((e.one - e.two) * (e.one - e.two), e.count);
		}
		return Math.sqrt(mean.mean());
	}
	
	/**
	 * This method controls how sparse sampling is done. To work with the
	 * implementation of TimeSeries, this should interpolate a value forwards in
	 * time
	 */
	private static Iterator<DataRun> compressData(Iterator<? extends SparseElement<? extends Number>> data,
			final long period, final long length) {
		checkArgument(period >= 1, "Period (%d) is not greater than or equal to 1", period);
		final PeekingIterator<? extends SparseElement<? extends Number>> peek = Iterators.peekingIterator(data);
		return Iterators.filter(new AbstractIterator<DataRun>() {
			@Override protected DataRun computeNext() {
				if (!peek.hasNext() || peek.peek().getIndex() >= length)
					return endOfData();
				SparseElement<? extends Number> val = peek.next();
				long nextIndex = Math.min(peek.hasNext() ? peek.peek().getIndex() : Long.MAX_VALUE, length);
				long count = nextIndex / period - val.getIndex() / period;
				return count > 0 ? new DataRun(val.getValue().doubleValue(), count) : null;
			}
		}, Predicates.notNull());
	}
	
	private static Iterable<DataRun> compressData(final Iterable<? extends SparseElement<? extends Number>> data,
			final long period, final long length) {
		return new Iterable<DataRun>() {
			@Override public Iterator<DataRun> iterator() { return compressData(data.iterator(), period, length); }
		};
	}
	
	/** This method controls how sparse sampling is done on two iterables */
	private static Iterator<DataPairRun> compressData(Iterator<? extends SparseElement<? extends Number>> first,
			Iterator<? extends SparseElement<? extends Number>> second, final long period, final long length) {
		checkArgument(period >= 1, "Period (%d) is not greater than or equal to 1", period);
		final PeekingIterator<? extends SparseElement<? extends Number>> it1 = Iterators.peekingIterator(first);
		final PeekingIterator<? extends SparseElement<? extends Number>> it2 = Iterators.peekingIterator(second);
		if (!it1.hasNext() || !it2.hasNext())
			return ImmutableList.<DataPairRun> of().iterator();
		return Iterators.filter(new AbstractIterator<DataPairRun>() {
			private SparseElement<? extends Number> first = it1.next();
			private SparseElement<? extends Number> second = it2.next();
			private long lastIndex = Math.max(first.getIndex(), second.getIndex());
			
			@Override protected DataPairRun computeNext() {
				if (lastIndex >= length)
					return endOfData();
				long nextIndex = Maths.min(it1.hasNext() ? it1.peek().getIndex() : Long.MAX_VALUE,
						it2.hasNext() ? it2.peek().getIndex() : Long.MAX_VALUE,
								length);
				long count = nextIndex / period - lastIndex / period;
				DataPairRun val = count > 0 ? new DataPairRun(first.getValue().doubleValue(), second.getValue().doubleValue(), count) : null;
				
				lastIndex = nextIndex;
				if (it1.hasNext() && it1.peek().getIndex() == nextIndex)
					first = it1.next();
				if (it2.hasNext() && it2.peek().getIndex() == nextIndex)
					second = it2.next();
				return val;
			}
		}, Predicates.notNull());
	}
	
	private static Iterable<DataPairRun> compressData(final Iterable<? extends SparseElement<? extends Number>> first,
			final Iterable<? extends SparseElement<? extends Number>> second, final long period, final long length) {
		return new Iterable<DataPairRun>() {
			@Override public Iterator<DataPairRun> iterator() { 
				return compressData(first.iterator(), second.iterator(), period, length);
			}
		};
	}

	private static class DataRun {
		private final double value;
		private final long count;
		private DataRun(double value, long count) {
			this.value = value;
			this.count = count;
		}
	}
	
	private static class DataPairRun {
		private final double one, two;
		private final long count;
		private DataPairRun(double one, double two, long count) {
			this.one = one;
			this.two = two;
			this.count = count;
		}
	}

	/** A sparse data point, some value with an index */
	public static class SparseElement<T> {
		private final T value;
		private final long index;
		
		private SparseElement(long index, T value) {
			this.index = index;
			this.value = value;
		}
		
		public static <E> SparseElement<E> create(long index, E element) {
			return new SparseElement<E>(index, element);
		}
		
		public T getValue() {
			return value;
		}
		
		public long getIndex() {
			return index;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || getClass() != other.getClass())
				return false;
			SparseElement<?> that = (SparseElement<?>) other;
			return this.index == that.index && Objects.equal(this.value, that.value);
		}
		
		@Override
		public int hashCode() {
			return Objects.hashCode(value, index);
		}

		@Override
		public String toString() {
			return value + " @ " + index;
		}
		
	}
	
}
