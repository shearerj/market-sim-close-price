package edu.umich.srg.util;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import edu.umich.srg.collect.SparseList.Entry;

import java.util.Iterator;
import java.util.stream.StreamSupport;

/**
 * Class for operating on sparse sets of data points. Typically these are time series objects.
 * 
 * In order to facilitate linear time in the the number of sparse data points, and not in terms of
 * the overall length of simulation, this interpolates all sparse data points forward in time until
 * a new sparse data point is found. This assumption isn't always accurate, but it is necessary for
 * compression.
 * 
 * A number of methods take a period and a length. The length is the total number of data points to
 * consider, inclusive of 0, but exclusive of "length". Period is the period to sample points at. A
 * period of 1 uses all of the points, a period of 100 will sample one point for every 100 units.
 * The point sampled is the point at the end of the interval. So a period of 100 will sample the
 * point at 99, 199, 299, etc.
 * 
 * @author erik
 * 
 */
public final class Sparse {

  /** Calculate the rmsd between two functions */
  public static double rmsd(Iterable<? extends Entry<? extends Number>> first,
      Iterable<? extends Entry<? extends Number>> second) {

    return Math.sqrt(StreamSupport.stream(compressData(first, second).spliterator(), false)
        .collect(SummStats::empty, (s, v) -> {
          s.acceptNTimes((v.one - v.two) * (v.one - v.two), v.count);
        } , SummStats::combine).getAverage());
  }

  /** This method controls how sparse sampling is done on two iterables */
  private static Iterable<DataPair> compressData(
      Iterable<? extends Entry<? extends Number>> firstIterable,
      Iterable<? extends Entry<? extends Number>> secondIterable) {
    PeekingIterator<? extends Entry<? extends Number>> firstIterator =
        Iterators.peekingIterator(firstIterable.iterator());
    PeekingIterator<? extends Entry<? extends Number>> secondIterator =
        Iterators.peekingIterator(secondIterable.iterator());

    if (!firstIterator.hasNext() || !secondIterator.hasNext())
      return ImmutableList.<DataPair>of();

    return () -> Iterators.filter(new Iterator<DataPair>() {
      private Entry<? extends Number> first = firstIterator.next();
      private Entry<? extends Number> second = secondIterator.next();
      private long lastIndex = Math.max(first.getIndex(), second.getIndex());

      @Override
      public boolean hasNext() {
        return firstIterator.hasNext() || secondIterator.hasNext();
      }

      @Override
      public DataPair next() {
        long nextIndex =
            Math.min(firstIterator.hasNext() ? firstIterator.peek().getIndex() : Long.MAX_VALUE,
                secondIterator.hasNext() ? secondIterator.peek().getIndex() : Long.MAX_VALUE);
        long count = nextIndex - lastIndex;

        DataPair val = count > 0 ? new DataPair(first.getElement().doubleValue(),
            second.getElement().doubleValue(), count) : null;

        lastIndex = nextIndex;
        if (firstIterator.hasNext() && firstIterator.peek().getIndex() == nextIndex)
          first = firstIterator.next();
        if (secondIterator.hasNext() && secondIterator.peek().getIndex() == nextIndex)
          second = secondIterator.next();
        return val;
      }
    }, Predicates.notNull());
  }

  private static class DataPair {
    private final double one, two;
    private final long count;

    private DataPair(double one, double two, long count) {
      this.one = one;
      this.two = two;
      this.count = count;
    }
  }

}
