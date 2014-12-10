package data;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import utils.Sparse.SparseElement;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import event.TimeStamp;

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
 * @author ewah
 * 
 */
public class TimeSeries implements Serializable, Iterable<SparseElement<Double>> {
	
	private static final long serialVersionUID = 7835744389750549565L;
	private static final Joiner joiner = Joiner.on(", ");
	
	protected final List<SparseElement<Double>> points;

	protected TimeSeries() {
		this.points = Lists.newArrayList();
	}

	public static TimeSeries create() {
		return new TimeSeries();
	}
	
	/**
	 * Add a data point (int, double) to container
	 * TODO time should be timestamp?
	 */
	public void add(TimeStamp time, double value) {
		long ticks = time.getInTicks();
		checkArgument((points.isEmpty() && ticks >= 0) || (!points.isEmpty() && ticks >= Iterables.getLast(points).getIndex()),
				"Can't insert values before 0 or previous time");
		if (!points.isEmpty() && Iterables.getLast(points).getIndex() == ticks)
			points.remove(points.size() - 1);
		if (points.isEmpty() || Iterables.getLast(points).getValue() != value) // Interpolate forward, so redundant
			points.add(SparseElement.create(ticks, value));
	}
	
	@Override
	public Iterator<SparseElement<Double>> iterator() {
		return Iterators.unmodifiableIterator(points.iterator());
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(getClass().equals(o.getClass())))
			return false;
		return this.points.equals(((TimeSeries) o).points);
	}

	@Override
	public String toString() {
		return "[" + joiner.join(points) + "]";
	}
	
}
