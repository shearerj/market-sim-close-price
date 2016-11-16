package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental.FundamentalView;

public class GaussianMeanRevertingView implements FundamentalView {

  private final Sim sim;
  private final Fundamental fundamental;
  private final long simLength;
  private final double mean;
  private final double kappac;

  private GaussianMeanRevertingView(Sim sim, Fundamental fundamental, long simLength, double mean,
      double meanReversion) {
    this.sim = sim;
    this.fundamental = fundamental;
    this.simLength = simLength;
    this.mean = mean;
    this.kappac = 1 - meanReversion;
  }

  public static GaussianMeanRevertingView create(Sim sim, Fundamental fundamental, long simLength,
      double mean, double meanReversion) {
    return new GaussianMeanRevertingView(sim, fundamental, simLength, mean, meanReversion);
  }

  @Override
  public double getEstimatedFinalFundamental() {
    long time = sim.getCurrentTime().get();
    double kappacToPower = Math.pow(kappac, simLength - time);
    return (1 - kappacToPower) * mean + kappacToPower * fundamental.getValueAt(time);
  }

}
