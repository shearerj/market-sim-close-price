package edu.umich.srg.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Ordering;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.PrimitiveIterator.OfDouble;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Compact Summary Statistics Class meant to only calculate sum and standard deviation. This
 * generally has similar accuracy than the Apache Commons Math SummaryStatistics class, but a better
 * API. Unlike the DoubleSummaryStatistics class provided by java, this also calculated squared
 * error and resulting statistics.
 */
public class SummStats implements DoubleConsumer, Consumer<Entry<? extends Number>> {

  private long count;
  private double average;
  private double squaredError;
  private double min;
  private double max;

  private SummStats(long count, double mean, double squaredError, double min, double max) {
    this.count = count;
    this.average = mean;
    this.squaredError = squaredError;
    this.min = min;
    this.max = max;
  }

  /** Create a new SumStats object with no data. */
  public static SummStats empty() {
    return new SummStats(0, 0, 0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
  }

  public static SummStats over(DoubleStream stream) {
    return stream.collect(SummStats::empty, SummStats::accept, SummStats::combine);
  }

  /** Compute summary stats over a collection. */
  public static SummStats over(Collection<? extends Number> data) {
    if (data instanceof Multiset<?>) {
      return ((Multiset<? extends Number>) data).entrySet().stream().collect(SummStats::empty,
          SummStats::accept, SummStats::combine);
    } else {
      return data.stream().collect(SummStats::empty, SummStats::accept, SummStats::combine);
    }
  }

  public static SummStats over(double... data) {
    return over(Arrays.stream(data));
  }

  @Override
  public void accept(double value) {
    count += 1;
    double delta = value - average;
    average += delta / count;
    squaredError += delta * (value - average);
    min = Math.min(min, value);
    max = Math.max(max, value);
  }

  public void accept(Number value) {
    accept(value.doubleValue());
  }

  @Override
  public void accept(Entry<? extends Number> entry) {
    acceptNTimes(entry.getElement().doubleValue(), entry.getCount());
  }

  /** Add several data points. */
  public void accept(Iterable<? extends Number> values) {
    for (Number n : values) {
      accept(n.doubleValue());
    }
  }

  /** Add several data points. */
  public void accept(OfDouble values) {
    while (values.hasNext()) {
      accept(values.nextDouble());
    }
  }

  /** Accept a data point several times. This is more efficient than calling accept n times. */
  public void acceptNTimes(double value, long times) {
    checkArgument(times >= 0);
    if (times > 0) {
      combine(new SummStats(times, value, 0, value, value));
    }
  }

  public void acceptNTimes(Number value, long times) {
    acceptNTimes(value.doubleValue(), times);
  }

  /** Return the mean of all data added so far. */
  public double getAverage() {
    return count == 0 ? Double.NaN : average;
  }

  /** Return the sample variance of all data added so far. */
  public double getVariance() {
    return count == 0 ? Double.NaN : count == 1 ? 0 : squaredError / (count - 1);
  }

  /** Return the sample standard deviation of all data added so far. */
  public double getStandardDeviation() {
    return Math.sqrt(getVariance());
  }

  public double getSum() {
    return getAverage() * count;
  }

  public double getMin() {
    return count == 0 ? Double.NaN : min;
  }

  public double getMax() {
    return count == 0 ? Double.NaN : max;
  }

  /** Return the number of data points. */
  public long getCount() {
    return count;
  }

  /** Merge other values into this. */
  public SummStats combine(SummStats other) {
    count += other.count;
    double delta = other.average - average;
    average += delta * other.count / count;
    squaredError += other.squaredError + delta * (other.average - average) * other.count;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
    return this;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof SummStats)) {
      return false;
    } else {
      SummStats that = (SummStats) other;
      return this.count == that.count && this.average == that.average && this.max == that.max
          && this.min == that.min && this.squaredError == that.squaredError;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, min, max, average, squaredError);
  }

  @Override
  public String toString() {
    return "<n: " + count + ", mean: " + average + ">";
  }

  // This could be made O(n) but isn't out of laziness and complexity
  /**
   * Compute median over a collection. O(n log n). Factor is in terms of distinct elements if
   * collection is a multiset.
   */
  public static double median(Collection<? extends Number> data) {
    if (data.isEmpty()) {
      return Double.NaN;
    } else if (data instanceof Multiset<?>) {
      List<Entry<? extends Number>> entries = ((Multiset<? extends Number>) data).entrySet()
          .stream().sorted(Ordering.natural().onResultOf(e -> e.getElement().doubleValue()))
          .collect(Collectors.toList());
      long[] sums = entries.stream().mapToLong(Entry::getCount).toArray();
      double[] values =
          entries.stream().map(Entry::getElement).mapToDouble(Number::doubleValue).toArray();

      // Cumsum
      long total = -1;
      for (int i = 0; i < sums.length; ++i) {
        total += sums[i];
        sums[i] = total;
      }

      // Find midpoint
      int index = Arrays.binarySearch(sums, (total + 1) / 2);
      if (index < 0) {
        index = -index - 1;
      }

      // Even and not on the line
      if (total % 2 == 1 && index > 0 && sums[index - 1] == (total - 1) / 2) {
        return (values[index - 1] + values[index]) / 2;
      } else {
        return values[index];
      }
    } else {
      // TODO This is inefficient, could be O(n)
      double[] sorted = data.stream().mapToDouble(Number::doubleValue).sorted().toArray();
      if (sorted.length % 2 == 0) {
        return (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2;
      } else {
        return sorted[sorted.length / 2];
      }
    }
  }

}
