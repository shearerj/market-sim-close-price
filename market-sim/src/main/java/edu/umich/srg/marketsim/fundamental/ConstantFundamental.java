package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.SparseList;
import edu.umich.srg.collect.SparseList.Entry;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.util.SummStats;

import java.util.Collections;
import java.util.Iterator;

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
  public Iterable<Entry<Number>> getFundamentalValues(long finalTime) {
    return Collections.singleton(SparseList.immutableEntry(0, constant));
  }

  @Override
  public double rmsd(Iterator<? extends Entry<? extends Number>> prices, long finalTime) {
    if (!prices.hasNext()) {
      return Double.NaN;
    }
    SummStats rmsd = SummStats.empty();
    Entry<? extends Number> nextPrice, lastPrice = prices.next();
    while (prices.hasNext() && (nextPrice = prices.next()).getIndex() <= finalTime) {
      double diff = lastPrice.getElement().doubleValue() - constant.doubleValue();
      rmsd.acceptNTimes(diff, nextPrice.getIndex() - lastPrice.getIndex());
      lastPrice = nextPrice;
    }
    double diff = lastPrice.getElement().doubleValue() - constant.doubleValue();
    rmsd.acceptNTimes(diff, finalTime - lastPrice.getIndex() + 1);
    return Math.sqrt(rmsd.getAverage());
  }

  private static final long serialVersionUID = 1;

}
