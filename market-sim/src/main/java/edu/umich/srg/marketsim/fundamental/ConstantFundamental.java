package edu.umich.srg.marketsim.fundamental;

import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.primitives.Ints;

import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;

import java.io.Serializable;
import java.util.Collections;
import java.util.Random;

public class ConstantFundamental implements Fundamental, Serializable {

  private final double constant;
  private final long finalTime;
  private final GaussianFundamentalView view;

  private ConstantFundamental(double constant, long finalTime) {
    this.constant = constant;
    this.finalTime = finalTime;
    this.view = new ConstantView();
  }

  public static ConstantFundamental create(double constant, long finalTime) {
    return new ConstantFundamental(constant, finalTime);
  }

  @Override
  public double getValueAt(long time) {
    return constant;
  }

  @Override
  public Iterable<Entry<Double>> getFundamentalValues() {
    // TODO set this so that multiple intmaxes are used in event of overflow
    return Collections
        .singleton(Multisets.immutableEntry(constant, Ints.checkedCast(finalTime + 1)));
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
