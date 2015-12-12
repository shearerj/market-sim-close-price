package edu.umich.srg.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;

import edu.umich.srg.collect.Streams;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;

/**
 * Compact Summary Statistics Class meant to only calculate sum and standard deviation. This
 * generally has more accuracy than the Apache Commons Math SummaryStatistics class.
 * 
 * Internally it is backed by the KahanSum class to do efficient sums of floating point values.
 * There are potentially more robust ways to do this, and this method is somewhat unproven, but it
 * seems to be more accurate than the method proposed by Knuth that is implemented many places
 * including Apache Commons Math.
 * 
 * @author erik
 * 
 */
public class CovarStats {

  private long count;
  private double xAverage, xSquaredError, xMin, xMax, yAverage, ySquaredError, yMin, yMax,
      xySquaredError;

  private CovarStats(long count, double xAverage, double xSquaredError, double xMin, double xMax,
      double yAverage, double ySquaredError, double yMin, double yMax, double xySquaredError) {
    this.count = count;
    this.xAverage = xAverage;
    this.xSquaredError = xSquaredError;
    this.xMin = xMin;
    this.xMax = xMax;
    this.yAverage = yAverage;
    this.ySquaredError = ySquaredError;
    this.yMin = yMin;
    this.yMax = yMax;
    this.xySquaredError = xySquaredError;
  }

  /**
   * Create a new CovarStats object with no data
   */
  public static CovarStats empty() {
    return new CovarStats(0, 0, 0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0,
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0);
  }

  public static CovarStats over(DoubleStream x, DoubleStream y) {
    return Streams.zip(x.boxed(), y.boxed(), (a, b) -> new double[] {a, b})
        .collect(new CovarStatsCollector());
  }

  public void accept(double x, double y) {
    count += 1;
    double xDelta = x - xAverage;
    double yDelta = y - yAverage;

    xAverage += xDelta / count;
    xSquaredError += xDelta * (x - xAverage);
    xMin = Math.min(xMin, x);
    xMax = Math.max(xMax, x);

    xySquaredError += yDelta * (x - xAverage);

    yAverage += yDelta / count;
    ySquaredError += yDelta * (y - yAverage);
    yMin = Math.min(yMin, y);
    yMax = Math.max(yMax, y);
  }

  public void acceptNTimes(double x, double y, long times) {
    checkArgument(times >= 0);
    if (times == 0)
      return;
    combine(new CovarStats(times, x, 0, x, x, y, 0, y, y, 0));
  }

  /**
   * Return the mean of all x data added so far
   */
  public double getXAverage() {
    return count == 0 ? Double.NaN : xAverage;
  }

  public double getYAverage() {
    return count == 0 ? Double.NaN : yAverage;
  }

  /**
   * Return the sample variance of all data added so far
   * 
   * @return
   */
  public double getXVariance() {
    return count == 0 ? Double.NaN : count == 1 ? 0 : xSquaredError / (count - 1);
  }

  public double getYVariance() {
    return count == 0 ? Double.NaN : count == 1 ? 0 : ySquaredError / (count - 1);
  }

  public double getCovariance() {
    return count == 0 ? Double.NaN : count == 1 ? 0 : xySquaredError / (count - 1);
  }

  /**
   * Return the sample standard deviation of all data added so far
   */
  public double getXStandardDeviation() {
    return Math.sqrt(getXVariance());
  }

  public double getYStandardDeviation() {
    return Math.sqrt(getYVariance());
  }

  public double getCoStandardDeviation() {
    return Math.sqrt(getCovariance());
  }

  public double getXSum() {
    return getXAverage() * count;
  }

  public double getYSum() {
    return getYAverage() * count;
  }

  public double getXMin() {
    return count == 0 ? Double.NaN : xMin;
  }

  public double getYMin() {
    return count == 0 ? Double.NaN : yMin;
  }

  public double getXMax() {
    return count == 0 ? Double.NaN : xMax;
  }

  public double getYMax() {
    return count == 0 ? Double.NaN : yMax;
  }

  /** Return the number of data points */
  public long getCount() {
    return count;
  }

  /** Merge other values into this */
  public CovarStats combine(CovarStats that) {
    count += that.count;
    double xDelta = that.xAverage - xAverage;
    double yDelta = that.yAverage - yAverage;

    xAverage += xDelta * that.count / count;
    xSquaredError += that.xSquaredError + xDelta * (that.xAverage - xAverage) * that.count;
    xMin = Math.min(xMin, that.xMin);
    xMax = Math.max(xMax, that.xMax);

    xySquaredError += that.xySquaredError + yDelta * (that.xAverage - xAverage) * that.count;

    yAverage += xDelta * that.count / count;
    ySquaredError += that.ySquaredError + yDelta * (that.yAverage - yAverage) * that.count;
    yMin = Math.min(yMin, that.yMin);
    yMax = Math.max(yMax, that.yMax);

    return this;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof CovarStats))
      return false;
    CovarStats that = (CovarStats) other;
    return this.count == that.count && this.xAverage == that.xAverage && this.xMax == that.xMax
        && this.xMin == that.xMin && this.xSquaredError == that.xSquaredError
        && this.yAverage == that.yAverage && this.yMax == that.yMax && this.yMin == that.yMin
        && this.ySquaredError == that.ySquaredError && this.xySquaredError == that.xySquaredError;
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, xMin, xMax, xAverage, xSquaredError, yMin, yMax, yAverage,
        ySquaredError, xySquaredError);
  }

  @Override
  public String toString() {
    return "<n: " + count + ", x mean: " + xAverage + ", y mean: " + yAverage + ">";
  }

  private static class CovarStatsCollector implements Collector<double[], CovarStats, CovarStats> {

    @Override
    public BiConsumer<CovarStats, double[]> accumulator() {
      return (cs, v) -> {
        cs.accept(v[0], v[1]);
      };
    }

    @Override
    public Set<Characteristics> characteristics() {
      return ImmutableSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
    }

    @Override
    public BinaryOperator<CovarStats> combiner() {
      return CovarStats::combine;
    }

    @Override
    public Function<CovarStats, CovarStats> finisher() {
      return Function.identity();
    }

    @Override
    public Supplier<CovarStats> supplier() {
      return CovarStats::empty;
    }

  }

}
