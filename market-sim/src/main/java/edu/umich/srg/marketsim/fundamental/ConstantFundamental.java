package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.collect.Sparse.Entry;

import java.io.Serializable;
import java.util.Collections;

public class ConstantFundamental implements Fundamental, Serializable {

  private final double constant;

  private ConstantFundamental(double constant) {
    this.constant = constant;
  }

  public static ConstantFundamental create(double constant) {
    return new ConstantFundamental(constant);
  }

  public static ConstantFundamental create(Number constant) {
    return new ConstantFundamental(constant.doubleValue());
  }

  @Override
  public double getValueAt(long time) {
    return constant;
  }

  @Override
  public Iterable<Entry<Double>> getFundamentalValues() {
    return Collections.singleton(Sparse.immutableEntry(0, constant));
  }

  private static final long serialVersionUID = 1;

}
