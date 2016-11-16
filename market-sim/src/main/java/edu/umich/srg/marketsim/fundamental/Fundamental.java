package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.Sparse.Entry;

/**
 * Class to store and compute a stochastic process used as a base to determine the private
 * valuations of agents.
 */
public interface Fundamental {

  double getValueAt(long time);

  Iterable<Entry<Double>> getFundamentalValues();

  interface FundamentalView {

    double getEstimatedFinalFundamental();

  }

}
