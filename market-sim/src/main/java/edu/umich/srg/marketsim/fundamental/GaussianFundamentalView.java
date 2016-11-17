package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;

import java.util.Random;

public interface GaussianFundamentalView extends FundamentalView {

  void addObservation(double observation, double variance, int quantity);

  interface GaussableView extends FundamentalView {

    GaussianFundamentalView addNoise(Random rand, double variance);

  }

}
