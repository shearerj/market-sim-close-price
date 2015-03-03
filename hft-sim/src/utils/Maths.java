package utils;

import static java.math.RoundingMode.HALF_EVEN;

import java.util.Arrays;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;

public abstract class Maths {

	// XXX This ignores points that have a NaN rmsd. This may not be desired.
	public static double rmsd(Iterator<? extends Number> iter_a, Iterator<? extends Number> iter_b) {
		SummStats mean = SummStats.on();
		while (iter_a.hasNext() && iter_b.hasNext()) {
			double a = iter_a.next().doubleValue();
			double b = iter_b.next().doubleValue();
			if (!Double.isNaN(a) && !Double.isNaN(b))
				mean.add((a - b) * (a - b));
		}
		return Math.sqrt(mean.mean());
	}
	
	public static double rmsd(Iterable<? extends Number> iter_a, Iterable<? extends Number> iter_b) {
		return rmsd(iter_a.iterator(), iter_b.iterator());
	}
	
	public static Iterator<Double> logRatio(final Iterator<? extends Number> values) {
		if (!values.hasNext()) {
			return ImmutableList.<Double> of().iterator();
		}

		return new UnmodifiableIterator<Double>() {
			double previous = Math.log(values.next().doubleValue());
		
			@Override
			public boolean hasNext() {
				return values.hasNext();
			}

			@Override
			public Double next() {
				double next = Math.log(values.next().doubleValue());
				double logr = next - previous;
				previous = next;
				return logr;
			}			
		};
	}
	
	public static Iterable<Double> logRatio(final Iterable<? extends Number> values) {
		return new Iterable<Double> () {
			@Override public Iterator<Double> iterator() {
				return logRatio(values.iterator());
			}
		};
	}
	
	public static Iterable<Double> logRatio(double... values) {
		return logRatio(Doubles.asList(values));
	}
	
	public static double stddev(Iterable<? extends Number> values) {
		return SummStats.on(values).stddev();
	}
	
	public static double stddev(double... values) {
		return stddev(Doubles.asList(values));
	}

	/** Quantize "n" in increments of "quanta" */
	public static int quantize(int n, int quanta) {
		return quanta * DoubleMath.roundToInt(n / (double) quanta, HALF_EVEN);
	}

	public static double quantize(double n, double quanta) {
		// Floor instead of round to prevent the case in round which converts
		// NaN and Inf to 0
		return quanta * DoubleMath.roundToInt(n / quanta, HALF_EVEN);
	}

	public static int bound(int num, int lower, int upper) {
		return Math.max(Math.min(num, upper), lower);
	}

	public static double bound(double num, double lower, double upper) {
		return Math.max(Math.min(num, upper), lower);
	}
	
	public static <T extends Number & Comparable<? super T>> T min(T... elements) {
		return Ordering.<T> natural().min(Arrays.asList(elements));
	}
	
	public static <T extends Number & Comparable<? super T>> T max(T... elements) {
		return Ordering.<T> natural().max(Arrays.asList(elements));
	}
	
	public static double median(double... values) {
		return Median.of(values).get();
	}
	
	public static double median(Iterable<? extends Number> values) {
		return Median.of(values).get();
	}
	
	public static class Median {
		/*
		 * These are technically balanced trees instead of heap, but the guava
		 * built-in functionality makes this too convenient. Asymptotic running
		 * time should be the same or better.
		 */
		private final SortedMultiset<Double> minHeap, maxHeap;
		
		private Median() {
			minHeap = TreeMultiset.<Double> create().descendingMultiset();
			maxHeap = TreeMultiset.create();
		}
		
		public static Median of(Iterator<? extends Number> vals) {
			Median med = new Median();
			while (vals.hasNext())
				med.add(vals.next().doubleValue());
			return med;
		}
		
		public static Median of(Iterable<? extends Number> vals) {
			return of(vals.iterator());
		}
		
		public static Median of(double... vals) {
			return of(Doubles.asList(vals));
		}
		
		public Median add(double val) {
			return add(val, 1);
		}
		
		public Median add(double val, int count) {
			// Looks messy, but adds to appropriate heap, checking for emptiness
			(((!minHeap.isEmpty() && minHeap.firstEntry().getElement() >= val) ||
					(!maxHeap.isEmpty() && maxHeap.firstEntry().getElement() > val))
					? minHeap : maxHeap).add(val, count);
			balance();
			return this;
		}
		
		public double get() {
			if (minHeap.isEmpty() && maxHeap.isEmpty())
				return Double.NaN;
			else if (minHeap.size() == maxHeap.size())
				return (minHeap.firstEntry().getElement() + maxHeap.firstEntry().getElement()) / 2;
			else
				return (minHeap.size() > maxHeap.size() ? minHeap : maxHeap).firstEntry().getElement();
		}
		
		private void balance() {
			SortedMultiset<Double> from = minHeap.size() > maxHeap.size() ? minHeap : maxHeap;
			SortedMultiset<Double> to = minHeap.size() > maxHeap.size() ? maxHeap : minHeap;			
			while (from.size() > to.size() + 1) {
				Entry<Double> e = from.pollFirstEntry();
				int swap = Math.min(from.size() - to.size(), e.getCount());
				swap += (e.getCount() - swap) / 2;
				to.add(e.getElement(), swap);
				from.add(e.getElement(), e.getCount() - swap);
			}
		}
		
	}
	
}
