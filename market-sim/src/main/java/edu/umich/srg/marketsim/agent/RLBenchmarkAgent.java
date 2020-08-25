package edu.umich.srg.marketsim.agent;

import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.BenchmarkDir;
import edu.umich.srg.marketsim.Keys.BenchmarkImpact;
import edu.umich.srg.marketsim.Keys.ContractHoldings;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.learning.BenchmarkState;

import java.util.Collection;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This agent gets noisy observations of the fundamental and uses markov assumptions of the price
 * series to get more refined estimates of the final fundamental.
 */
public class RLBenchmarkAgent extends DeepRLAgent {
	  
	private final BenchmarkState stateSpace;
  
	private final int benchmarkImpact;
    private final double contractHoldings;
    private final int benchmarkDir;
  
  /** Standard constructor for ZIR agent. */
  public RLBenchmarkAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
	super(sim,market,fundamental,spec,rand);
    
    this.stateSpace = BenchmarkState.create(this.sim,this.market,spec);
      
    this.benchmarkImpact = spec.get(BenchmarkImpact.class);
    this.contractHoldings = spec.get(ContractHoldings.class);
    this.benchmarkDir = spec.get(BenchmarkDir.class);
  
  }
  
  public static RLBenchmarkAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new RLBenchmarkAgent(sim, market, fundamental, spec, rand);
  }
  
  @Override
  protected JsonArray getState(double finalEstimate, int side) {
	  return this.stateSpace.getState(finalEstimate, side, privateValue);
  }
  
  @Override
  protected JsonObject getStateDict(double finalEstimate, int side) {
	  return this.stateSpace.getStateDict(finalEstimate, side, privateValue);
  }
  
  @Override
  protected JsonArray getNormState(double finalEstimate, int side) {
	  JsonArray fullState = this.stateSpace.getState(finalEstimate, side, privateValue);
	  return this.stateSpace.getNormState(fullState);
  }
 
  @Override
  protected double calculateReward(double finalEstimate) {
	  double currProfit = super.calculateReward(finalEstimate);
	  double currBenchmark = market.getCurrentBenchmark();
	  return currProfit + (currBenchmark * this.benchmarkDir * this.contractHoldings);
	  //return currProfit + (currBenchmark * this.benchmarkDir);
	  
  }
  
  protected int getBenchmarkImpact() {
    return this.benchmarkDir * this.benchmarkImpact;
  }
  
  protected int benchmarkDirection() {
	  return this.benchmarkDir;
  }
  
  protected double benchmarkHoldings() {
	  return this.contractHoldings;
  }

  protected String name() {
    return "RLBenchmark";
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