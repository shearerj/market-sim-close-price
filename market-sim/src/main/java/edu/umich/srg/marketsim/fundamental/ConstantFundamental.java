package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.collect.Sparse.Entry;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

import java.util.Collections;

public class ConstantFundamental extends GaussianMeanReverting {

  private final Price constant;

  private ConstantFundamental(Price constant) {
    this.constant = constant;
  }

  public static ConstantFundamental create(Price constant) {
    return new ConstantFundamental(constant);
  }

  public static ConstantFundamental create(Number constant) {
    return new ConstantFundamental(Price.of(constant.doubleValue()));
  }

  @Override
  public Price getValueAt(TimeStamp time) {
    return constant;
  }

  @Override
  public Iterable<Entry<Number>> getFundamentalValues(TimeStamp finalTime) {
    return Collections.singleton(Sparse.immutableEntry(0, constant));
  }

  private static final long serialVersionUID = 1;

}
