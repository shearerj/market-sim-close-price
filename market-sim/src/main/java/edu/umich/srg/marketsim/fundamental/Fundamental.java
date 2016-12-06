package edu.umich.srg.marketsim.fundamental;

import com.google.common.collect.Multiset.Entry;

import edu.umich.srg.marketsim.Sim;

/**
 * Class to store and compute a stochastic process used as a base to determine the private
 * valuations of agents.
 */
public interface Fundamental {

  /** Gets the fundamental value at the specific time. */
  double getValueAt(long time);

  /** Returns a compact representation of the fundamental. */
  Iterable<Entry<Double>> getFundamentalValues();

  FundamentalView getView(Sim sim);

  interface FundamentalView {

    double getEstimatedFinalFundamental();

  }

}
