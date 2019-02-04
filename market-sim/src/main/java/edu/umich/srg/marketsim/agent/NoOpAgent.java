package edu.umich.srg.marketsim.agent;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;

import java.util.Collection;
import java.util.Random;

public class NoOpAgent implements Agent {

  public static NoOpAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new NoOpAgent();
  }

  @Override
  public void initilaize() {}

  @Override
  public int getId() {
    return 0;
  }
  
  @Override
  public int getBenchmarkDir() {
	return 0;
  }
  
  @Override
  public double getContractHoldings() {
	  return 0;
  }

  @Override
  public double payoffForExchange(int position, OrderType type) {
    return 0;
  }

  @Override
  public String toString() {
    return "NOOP";
  }

}
