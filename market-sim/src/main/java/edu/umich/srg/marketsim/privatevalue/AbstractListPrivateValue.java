package edu.umich.srg.marketsim.privatevalue;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;
import edu.umich.srg.fourheap.Order.OrderType;

import java.util.Arrays;
import java.util.Random;

abstract class AbstractListPrivateValue implements PrivateValue {

  private final int offset;
  private final double[] values;

  AbstractListPrivateValue(double[] values, int offset) {
    this.values = values;
    this.offset = offset;
  }

  AbstractListPrivateValue(double[] values) {
    this(values, values.length / 2);
  }

  AbstractListPrivateValue(DoubleDistribution dist, int maxPosition, Random rand) {
    this(generateValues(dist, maxPosition, rand), maxPosition);
  }

  @Override
  public double valueForExchange(int position, OrderType type) {
    return valueAtPosition(position + type.sign()) - valueAtPosition(position);
  }

  @Override
  public double valueAtPosition(int position) {
    return values[Math.max(0, Math.min(position + offset, values.length - 1))];
  }

  private static double[] generateValues(DoubleDistribution dist, int maxPosition, Random rand) {
    // Randomly populate large array with 2 * maxPosition elements
    double[] values = new double[2 * maxPosition + 1];
    for (int i = 1; i < values.length; ++i)
      values[i] = dist.sample(rand);
    // Sort them descending (sort + reverse)
    Arrays.sort(values, 1, values.length);
    for (int i = 1; i <= maxPosition; ++i) {
      double first = values[i];
      values[i] = values[values.length - i];
      values[values.length - i] = first;
    }
    // Cumulatively sum them
    for (int i = 1; i < values.length; ++i)
      values[i] += values[i - 1];
    // Subtract the offset so that valueAtPosition(0) == 0
    double median = values[maxPosition];
    for (int i = 0; i < values.length; ++i)
      values[i] -= median;
    return values;
  }

}
