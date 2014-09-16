package data;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.List;

import utils.Iterables2;
import utils.Iterators2;
import utils.SparseIterable;
import utils.SparseIterator;
import utils.SparseIterator.SparseElement;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Storage for time series objects.
 * 
 * Keeps track of the data points that are added to the time series and the time
 * they're added. Also expands it into a full list of values (with an element
 * for each time).
 * 
 * When filling in values, a new element is added to series if the time for the
 * new data point is different from the most recent time recorded.
 * 
 * TODO Would like to have the methods just return views instead of copying the
 * data :( This shouldn't be too hard. Methods should just return Iterables
 * instead of Lists
 * 
 * @author ewah
 * 
 */
public class TimeSeries implements Serializable, SparseIterable<Double> {
	
	private static final long serialVersionUID = 7835744389750549565L;
	private static final Joiner joiner = Joiner.on(", ");
	private static final SparseElement<Double> zero = SparseElement.create(0, Double.NaN);
	
	protected final List<SparseElement<Double>> points;

	protected TimeSeries() {
		this.points = Lists.newArrayList();
		add(0, Double.NaN);
	}

	public static TimeSeries create() {
		return new TimeSeries();
	}

	@Override
	public String toString() {
		return "[" + joiner.join(points) + "]";
	}

	/**
	 * Add a data point (int, double) to container
	 */
	public void add(long time, double value) {
		long lastTime = Iterables.getLast(points, zero).index;
		checkArgument(time >= lastTime, "Can't add time before last time");
		
		points.add(SparseElement.create(time, value));
	}
	
	/**
	 * Returns an iterable view backed by the underlying data, but without any
	 * nans.
	 * 
	 * Note, this can cause issues, as the default value is nan, and so can
	 * result in null elements as you sample with no data.
	 */
	public Iterable<Double> removeNans() {
		return filter(Predicates.not(Predicates.equalTo(Double.NaN)));
	}
	
	/**
	 * Same as removeNans, but with an arbitrary predicate.
	 */
	public Iterable<Double> filter(final Predicate<Double> predicate) {
		return Iterables2.fromSparse(Iterables.unmodifiableIterable(
				Iterables.filter(points, new Predicate<SparseElement<Double>>() {
					@Override
					public boolean apply(SparseElement<Double> input) {
						return predicate.apply(input.element);
					}
				})));
	}

	@Override
	public SparseIterator<Double> iterator() {
		return Iterators2.fromSparse(Iterators.unmodifiableIterator(points.iterator()));
	}

}
