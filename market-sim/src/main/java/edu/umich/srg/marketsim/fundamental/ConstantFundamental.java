package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.SparseList;
import edu.umich.srg.collect.SparseList.Entry;
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
  public FundamentalInfo getInfo() {
    return new FundamentalInfo() {

      @Override
      public Iterable<Entry<Number>> getFundamentalValues() {
        return Collections.singleton(SparseList.immutableEntry(0, constant));
      }

    };
  }

  private static final long serialVersionUID = 1;

}
