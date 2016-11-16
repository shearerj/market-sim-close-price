package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.collect.Sparse.Entry;
import edu.umich.srg.marketsim.Sim;

import java.io.Serializable;
import java.util.Collections;

public class ConstantFundamental implements Fundamental, Serializable {

  private final double constant;
  private final FundamentalView view;

  private ConstantFundamental(double constant) {
    this.constant = constant;
    this.view = new ConstantView();
  }

  public static ConstantFundamental create(double constant) {
    return new ConstantFundamental(constant);
  }

  @Override
  public double getValueAt(long time) {
    return constant;
  }

  @Override
  public Iterable<Entry<Double>> getFundamentalValues() {
    return Collections.singleton(Sparse.immutableEntry(0, constant));
  }

  @Override
  public FundamentalView getView(Sim sim) {
    return view;
  }

  private class ConstantView implements FundamentalView {

    @Override
    public double getEstimatedFinalFundamental() {
      return constant;
    }

  }

  private static final long serialVersionUID = 1;

}
