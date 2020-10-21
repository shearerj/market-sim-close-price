package edu.umich.srg.marketsim.agent;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.BenchmarkDir;
import edu.umich.srg.marketsim.Keys.ContractHoldings;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;

import java.util.Collection;
import java.util.Random;

/**
 * This agent gets noisy observations of the fundamental and uses markov assumptions of the price
 * series to get more refined estimates of the final fundamental.
 */
public class BenchmarkTensorFlowRLAgent extends TensorFlowRLAgent {
	  
	//private final BenchmarkState stateSpace;
    private final double contractHoldings;
    private final int benchmarkDir;
  
  /** Standard constructor for ZIR agent. */
  public BenchmarkTensorFlowRLAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
	super(sim,market,fundamental,spec,rand);
    
    //this.stateSpace = BenchmarkState.create(this.sim,this.market,spec);
	
    this.contractHoldings = spec.get(ContractHoldings.class);
    this.benchmarkDir = spec.get(BenchmarkDir.class);
  
  }
  
  public static BenchmarkTensorFlowRLAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new BenchmarkTensorFlowRLAgent(sim, market, fundamental, spec, rand);
  }
 
  @Override
  protected double calculateReward(double finalEstimate) {
	  double currProfit = super.calculateReward(finalEstimate);
	  //System.out.println(currProfit);
	  
	  double currBenchmark = market.getCurrentBenchmark();
	  
	  //System.out.println(currBenchmark);
	  //System.out.println(currProfit + (currBenchmark * this.benchmarkDir * this.contractHoldings));
	  
	  return currProfit + (currBenchmark * this.benchmarkDir * this.contractHoldings);
	  
  }
  
  protected int benchmarkDirection() {
	  return this.benchmarkDir;
  }
  
  protected double benchmarkHoldings() {
	  return this.contractHoldings;
  }

  protected String name() {
    return "BenchmarkTensorFlowRL";
  }
  
  @Override
  public int getBenchmarkDir() {
	return benchmarkDirection();
  }
  
  @Override
  public double getContractHoldings() {
	return benchmarkHoldings();
  }
  

}