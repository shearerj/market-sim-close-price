package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.collect.Sparse.Entry;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;

import java.io.Serializable;
import java.util.Collections;
import java.util.Random;

public class ConstantFundamental implements Fundamental, Serializable {

  private final double constant;
  private final GaussianFundamentalView view;

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
  public GaussianFundamentalView getView(Sim sim) {
    return view;
  }

  private class ConstantView implements GaussableView, GaussianFundamentalView {

    @Override
    public double getEstimatedFinalFundamental() {
      return constant;
    }

    @Override
    public void addObservation(double observation, double variance, int quantity) {}

    @Override
    public GaussianFundamentalView addNoise(Random rand, double variance) {
      return this;
    }

  }

  private static final long serialVersionUID = 1;

}
