package edu.umich.srg.marketsim.agent;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.BenchmarkDir;
import edu.umich.srg.marketsim.Keys.BenchmarkImpact;
import edu.umich.srg.marketsim.Keys.ContractHoldings;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PolicyAction;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.ShareEstimates;
import edu.umich.srg.marketsim.Keys.Sides;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.SubmitDepth;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.observer.MarkovObserver;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.strategy.SharedGaussianView;
import edu.umich.srg.util.SummStats;
import edu.umich.srg.learning.BenchmarkState;
import edu.umich.srg.learning.ContinuousAction;
import edu.umich.srg.learning.PythonContinuousAction;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This agent gets noisy observations of the fundamental and uses markov assumptions of the price
 * series to get more refined estimates of the final fundamental.
 */
public class RLBenchmarkAgentOld implements Agent {

	private static final Distribution<OrderType> randomOrder = Uniform.over(OrderType.values());
	private static final Set<OrderType> allOrders = EnumSet.allOf(OrderType.class);
	
	protected final Sim sim;
	protected final Random rand;
	private final int id;
	private final MarketView market;
	private int maxPosition;
	  private PrivateValue privateValue;
	  private final Geometric arrivalDistribution;
	  private final Supplier<Set<OrderType>> side;
	  private final int ordersPerSide;
	
	  // Bookkeeping
	  private final double finalFundamental;
	  private final SummStats shadingStats;
	  private final SummStats fundamentalError;
	
	  private final GaussianFundamentalView fundamental;
	  private final IntUniform shadingDistribution;
	  
	  private double prevProfit;
	  private final boolean policyAction;
	  
	  private final BenchmarkState stateSpace;
	  //private final SimpleState stateSpace;
	  private final ContinuousAction randomActionSpace;
	  private final PythonContinuousAction policyActionSpace;
	  
	  private Boolean firstArrival;
	  
	  private final JsonArray rl_observations;
	  private JsonObject prev_obs;
	  private JsonArray action;
  
	private final int benchmarkImpact;
    private final double contractHoldings;
    private final int benchmarkDir;
  
  /** Standard constructor for ZIR agent. */
  public RLBenchmarkAgentOld(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
	this.sim = sim;
    this.id = rand.nextInt();
    this.market = market.getView(this, TimeStamp.ZERO);
    this.maxPosition = spec.get(MaxPosition.class);
    this.privateValue = PrivateValues.gaussianPrivateValue(rand, spec.get(MaxPosition.class),
        spec.get(PrivateValueVar.class));
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    switch (spec.get(Sides.class)) {
      case RANDOM:
        this.side = () -> Collections.singleton(randomOrder.sample(rand));
        break;
      case BOTH:
        this.side = () -> allOrders;
        break;
      default:
        throw new IllegalArgumentException("Sides was null");
    }
    this.ordersPerSide = spec.get(SubmitDepth.class);
    this.rand = rand;

    this.finalFundamental = fundamental.getValueAt(spec.get(SimLength.class));
    this.shadingStats = SummStats.empty();
    this.fundamentalError = SummStats.empty();
    
    if (spec.get(ShareEstimates.class)) {
        this.fundamental = SharedGaussianView.create(sim, fundamental, rand,
            spec.get(FundamentalObservationVariance.class));
    } else {
        this.fundamental = ((GaussableView) fundamental.getView(sim)).addNoise(rand,
            spec.get(FundamentalObservationVariance.class));
    }
      
    this.policyAction = spec.get(PolicyAction.class);
    
    this.stateSpace = BenchmarkState.create(this.sim,this.market,spec);
    //this.stateSpace = SimpleState.create(this.sim,this.market,spec);
    this.randomActionSpace = ContinuousAction.create(spec,rand);
    this.policyActionSpace = PythonContinuousAction.create(spec,rand);
  
    this.maxPosition = spec.get(MaxPosition.class);
    this.privateValue = PrivateValues.gaussianPrivateValue(rand, spec.get(MaxPosition.class),
            spec.get(PrivateValueVar.class));
  
    this.rl_observations = new JsonArray();
    this.firstArrival = true;
    this.prev_obs = new JsonObject();
    
    this.action = new JsonArray();
  
    this.shadingDistribution = Uniform.closed(spec.get(Rmin.class), spec.get(Rmax.class));
    double priceVarEst = spec.get(PriceVarEst.class);
    if (Double.isFinite(priceVarEst)) {
      market.addTransactionObserver(MarkovObserver.create(this.fundamental, priceVarEst));
    }
      
    this.benchmarkImpact = spec.get(BenchmarkImpact.class);
    this.contractHoldings = spec.get(ContractHoldings.class);
    this.benchmarkDir = spec.get(BenchmarkDir.class);
  
  }
  
  public static RLBenchmarkAgentOld createFromSpec(Sim sim, Fundamental fundamental,
	      Collection<Market> markets, Market market, Spec spec, Random rand) {
	    return new RLBenchmarkAgentOld(sim, market, fundamental, spec, rand);
	  }
  
  private void scheduleNextArrival() {
	    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
	  }

  protected final void strategy() {
    ImmutableList.copyOf(market.getActiveOrders().entrySet()).forEach(market::withdrawOrder);
    
    JsonObject curr_obs = new JsonObject();

    Set<OrderType> sides = side.get();
    double finalEstimate = getFinalFundamentalEstiamte();
    fundamentalError.accept(Math.pow(finalEstimate - finalFundamental, 2));
    
    JsonArray state = this.stateSpace.getState(finalEstimate, privateValue);
    double currProfit = this.calculateReward(finalEstimate);
    
    for (OrderType type : sides) {
      state.add(type.sign());
	  for (int num = 0; num < ordersPerSide; num++) {
	    if (Math.abs(market.getHoldings() + (num + 1) * type.sign()) <= maxPosition) {
	
	      double privateBenefit = type.sign()
	          * privateValue.valueForExchange(market.getHoldings() + num * type.sign(), type);
	      double estimatedValue = finalEstimate + privateBenefit;
 
	      JsonObject curr_state = new JsonObject();
	      curr_state.add("state0", state);
	      double toSubmit = this.getAction(finalEstimate, curr_state.toString());
	      if (toSubmit < 0) { // Hacky patch to stop submiting
	        continue;
	      }
	      long rounded = DoubleMath.roundToLong(toSubmit, type == BUY ? FLOOR : CEILING);
	      shadingStats.accept(Math.abs(estimatedValue - toSubmit));
	
	      if (rounded > 0) {
	    	  market.submitOrder(type, Price.of(rounded), 1);
	        //action.addProperty("side", type.sign());
	      }
  	    }
  	  }
    }
    
    curr_obs.add("state0", state);
    //curr_obs.add("state2", state2);
    curr_obs.add("action", this.action);
    prev_obs.add("state1", state);
    prev_obs.addProperty("terminal", 0);
    
    if (firstArrival) {
        firstArrival = false;
      } else {
        double reward = currProfit - this.prevProfit;
        this.prev_obs.addProperty("reward", reward);
        this.rl_observations.add(prev_obs);
      }
    this.prevProfit = currProfit;
    
    this.prev_obs = curr_obs;

    scheduleNextArrival();
  }
  
  protected double getAction(double finalEstimate, String curr_state) {
      double toSubmit = 0;
      
      if(this.policyAction) {
    	  this.action = this.policyActionSpace.getAction(curr_state);
    	  toSubmit = this.policyActionSpace.actionToPrice(finalEstimate);
      }
      else { 
    	  this.action = this.randomActionSpace.getAction();
    	  toSubmit = this.randomActionSpace.actionToPrice(finalEstimate);
	  }
	  return toSubmit;
  }
  
  protected double calculateReward(double finalEstimate) {
	  double estProfit = market.getProfit();
	  int market_h = market.getHoldings();
	  double currBenchmark = market.getCurrentBenchmark();
	  
	  estProfit += market_h * finalEstimate;
	  OrderType direction = market_h > 0 ? BUY : SELL;
	  for (int pos = 0; pos != market_h; pos += direction.sign()) {
		  estProfit += this.payoffForExchange(pos, direction);
	  }
	  
	  return estProfit + (currBenchmark * this.benchmarkDir);
	  
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
    return "RLBenchmarkOld";
  }
  
  @Override
  public int getBenchmarkDir() {
	return benchmarkDirection();
  }
  
  @Override
  public double getContractHoldings() {
	return benchmarkHoldings();
  }
  
  @Override
  public final void initilaize() {
    scheduleNextArrival();
  }

  @Override
  public final double payoffForExchange(int position, OrderType type) {
    return privateValue.valueForExchange(position, type);
  }

  @Override
  public JsonObject getFeatures() {
    JsonObject feats = Agent.super.getFeatures();
    feats.addProperty("count_shading", shadingStats.getCount());
    feats.addProperty("mean_shading", shadingStats.getAverage().orElse(0.0));
    feats.addProperty("arrivals", fundamentalError.getCount());
    feats.addProperty("mean_fundamental_error", fundamentalError.getAverage().orElse(0.0));
    this.finalRlObs();
    feats.add("rl_observations", this.rl_observations);
    return feats;
  }
  
  private void finalRlObs() {
    double currProfit = this.calculateReward(this.finalFundamental);
    double reward = currProfit - this.prevProfit;
   
    JsonArray state = this.stateSpace.getState(this.finalFundamental, privateValue);
    state.add(0);
    this.prev_obs.add("state1", state);
    this.prev_obs.addProperty("terminal", 1);
    this.prev_obs.addProperty("reward", reward);
    this.rl_observations.add(prev_obs);
  }

  protected double getDesiredSurplus() {
    return shadingDistribution.sample(rand);
  }

  protected double getFinalFundamentalEstiamte() {
    return fundamental.getEstimatedFinalFundamental();
  }
  
  @Override
  public final int getId() {
    return id;
  }

  @Override
  public final String toString() {
    return name() + " " + Integer.toUnsignedString(id, 36).toUpperCase();
  }

}