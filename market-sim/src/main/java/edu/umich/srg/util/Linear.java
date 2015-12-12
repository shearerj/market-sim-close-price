package edu.umich.srg.util;

import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

public interface Linear {

  public static DoubleUnaryOperator linearFit(DoubleStream x, DoubleStream y) {
    CovarStats cs = CovarStats.over(x, y);
    double slope = cs.getCovariance() / cs.getXVariance(),
        intercept = cs.getYAverage() - slope * cs.getXAverage();
    return d -> d * slope + intercept;
  }

  public static double rSquared(DoubleStream x, DoubleStream y) {
    CovarStats cs = CovarStats.over(x, y);
    return cs.getCovariance() * cs.getCovariance() / (cs.getXVariance() * cs.getYVariance());
  }

}
