package edu.umich.srg.marketsim.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.BenchmarkDir;
import edu.umich.srg.marketsim.Keys.BenchmarkImpact;
import edu.umich.srg.marketsim.Keys.ContractHoldings;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.ShareEstimates;
import edu.umich.srg.marketsim.Keys.Sides;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.SubmitDepth;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.Keys.ViewBookDepth;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.BenchmarkReward;
import edu.umich.srg.marketsim.Keys.PolicyAction;
import edu.umich.srg.marketsim.Keys.RandomActionVar;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.observer.GetQuoteObserver;
import edu.umich.srg.marketsim.observer.MarkovObserver;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.strategy.SharedGaussianView;
import edu.umich.srg.marketsim.strategy.SurplusThreshold;
import edu.umich.srg.util.SummStats;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * This agent gets noisy observations of the fundamental and uses markov assumptions of the price
 * series to get more refined estimates of the final fundamental.
 */
public class RLBenchmarkAgent implements Agent {
	
  private static final Distribution<OrderType> randomOrder = Uniform.over(OrderType.values());
  private static final Set<OrderType> allOrders = EnumSet.allOf(OrderType.class);

  protected final Sim sim;
  protected final Random rand;
  private final int id;
  private final MarketView market;
  private final GetQuoteObserver quoteInfo;
  private int maxPosition;
  private final SurplusThreshold threshold;
  private PrivateValue privateValue;
  private final Geometric arrivalDistribution;
  private final Supplier<Set<OrderType>> side;
  private final int ordersPerSide;
  private final Long timeHorizon;

  // Bookkeeping
  private final double finalFundamental;
  private final SummStats shadingStats;
  private final SummStats fundamentalError;

  private final GaussianFundamentalView fundamental;
  private final IntUniform shadingDistribution;
  
  private final int benchmarkImpact;
  private final double contractHoldings;
  private final int benchmarkDir;
  private final int bookDepth;
  private double prevBenchmark;
  private final int benchmarkReward;
  private final boolean policyAction;
  private final double randomActionVar;
  private Boolean firstArrival;
  
  private final double observationVar;
  
  private final JsonArray rl_observations;
  private JsonObject prev_obs;
  
  /** Standard constructor for ZIR agent. */
  public RLBenchmarkAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.id = rand.nextInt();
    this.market = market.getView(this, TimeStamp.ZERO);
    this.quoteInfo = market.addQuoteObserver(GetQuoteObserver.create(market));
    this.maxPosition = spec.get(MaxPosition.class);
    this.threshold = SurplusThreshold.create(spec.get(Thresh.class));
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
      
    this.benchmarkImpact = spec.get(BenchmarkImpact.class);
    this.contractHoldings = spec.get(ContractHoldings.class);
    this.benchmarkDir = spec.get(BenchmarkDir.class);
    this.bookDepth = spec.get(ViewBookDepth.class);
    this.benchmarkReward = spec.get(BenchmarkReward.class);
    this.prevBenchmark = spec.get(FundamentalMean.class);
    this.timeHorizon = spec.get(SimLength.class);
    this.observationVar = spec.get(FundamentalObservationVariance.class);
    this.policyAction = spec.get(PolicyAction.class);
    this.randomActionVar = spec.get(RandomActionVar.class);
  
    this.maxPosition = spec.get(MaxPosition.class);
    this.privateValue = PrivateValues.gaussianPrivateValue(rand, spec.get(MaxPosition.class),
            spec.get(PrivateValueVar.class));
  
    this.rl_observations = new JsonArray();
    this.firstArrival = true;
    this.prev_obs = new JsonObject();
  
    this.shadingDistribution = Uniform.closed(spec.get(Rmin.class), spec.get(Rmax.class));
    double priceVarEst = spec.get(PriceVarEst.class);
    if (Double.isFinite(priceVarEst)) {
      market.addTransactionObserver(MarkovObserver.create(this.fundamental, priceVarEst));
    }
  }
  
  public static RLBenchmarkAgent createFromSpec(Sim sim, Fundamental fundamental,
	      Collection<Market> markets, Market market, Spec spec, Random rand) {
	    return new RLBenchmarkAgent(sim, market, fundamental, spec, rand);
	  }
  
  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  protected final void strategy() {
    ImmutableList.copyOf(market.getActiveOrders().entrySet()).forEach(market::withdrawOrder);
    
    JsonObject curr_obs = new JsonObject();
    JsonObject state = this.stateSpace();
    JsonArray stateArray = this.stateSpaceArray();
    JsonObject action = new JsonObject();

    Set<OrderType> sides = side.get();
    double finalEstimate = getFinalFundamentalEstiamte();
    fundamentalError.accept(Math.pow(finalEstimate - finalFundamental, 2));
    double demandedSurplus = getDesiredSurplus();
    
    double estProfit = market.getProfit();
    int market_h = market.getHoldings();
    double currBenchmark = market.getCurrentBenchmark();
    int num_transactions = market.getCurrentNumTransactions();
    
    for (OrderType type : sides) {
      state.addProperty("side", type.sign());
      stateArray.add(type.sign());
      if(this.policyAction) {
	    for (int num = 0; num < ordersPerSide; num++) {
	      if (Math.abs(market.getHoldings() + (num + 1) * type.sign()) <= maxPosition) {
	
	        double privateBenefit = type.sign()
	            * privateValue.valueForExchange(market.getHoldings() + num * type.sign(), type);
	        double estimatedValue = finalEstimate + privateBenefit;
	
	        demandedSurplus -= type.sign() * getBenchmarkImpact();
	        double toSubmit = threshold.shadePrice(type, quoteInfo.getQuote(), estimatedValue, demandedSurplus);
	        if (toSubmit < 0) { // Hacky patch to stop submiting
	          continue;
	        }
	        long rounded = DoubleMath.roundToLong(toSubmit, type == BUY ? FLOOR : CEILING);
	        shadingStats.accept(Math.abs(estimatedValue - toSubmit));
	        //Optional<Price> marketPrice = type == BUY ? quoteInfo.getQuote().getAskPrice() : quoteInfo.getQuote().getBidPrice();
	
	        if (rounded > 0) {
	          market.submitOrder(type, Price.of(rounded), 1);
	          //orderSign = type.sign();
	            
	          //action.addProperty("side", type.sign());
	          action.addProperty("price", rounded);
	            
	          //if(marketPrice.isPresent()) {
	            //if(type.sign() * rounded >= type.sign() * marketPrice.get().longValue()) {
	              //estProfit -= marketPrice.get().longValue() * type.sign();
	              //market_h += type.sign();
	              //currBenchmark = (currBenchmark * num_transactions + marketPrice.get().longValue()) / (num_transactions + 1);
	            //}
	          //}
	        }
	      }
	    }
	  } else {
  	    for (int num = 0; num < ordersPerSide; num++) {
  	      if (Math.abs(market.getHoldings() + (num + 1) * type.sign()) <= maxPosition) {
  	
  	        double privateBenefit = type.sign()
  	            * privateValue.valueForExchange(market.getHoldings() + num * type.sign(), type);
  	        double estimatedValue = finalEstimate + privateBenefit;
  	        
  	        Gaussian pricesToSubmit = Gaussian.withMeanVariance(estimatedValue, this.randomActionVar);
  	        double toSubmit = pricesToSubmit.sample(rand);
  	        if (toSubmit < 0) { // Hacky patch to stop submiting
  	          continue;
  	        }
  	        long rounded = DoubleMath.roundToLong(toSubmit, type == BUY ? FLOOR : CEILING);
  	        //Optional<Price> marketPrice = type == BUY ? quoteInfo.getQuote().getAskPrice() : quoteInfo.getQuote().getBidPrice();
  	
  	        if (rounded > 0) {
  	          market.submitOrder(type, Price.of(rounded), 1);
  	          //orderSign = type.sign();
  	            
  	          //action.addProperty("side", type.sign());
  	          action.addProperty("price", rounded);
  	            
  	          //if(marketPrice.isPresent()) {
  	            //if(type.sign() * rounded >= type.sign() * marketPrice.get().longValue()) {
  	              //estProfit -= marketPrice.get().longValue() * type.sign();
  	              //market_h += type.sign();
  	              //currBenchmark = (currBenchmark * num_transactions + marketPrice.get().longValue()) / (num_transactions + 1);
  	            //}
  	          //}
  	        }
  	      }
  	    }
  	  }
    }
    
    curr_obs.add("state", state);
    curr_obs.add("stateArray", stateArray);
    curr_obs.add("action", action);
    //prev_obs.add("state1", state);
    prev_obs.addProperty("terminal", 0);
    
    estProfit += market_h * finalEstimate;
    OrderType direction = market_h > 0 ? BUY : SELL;
    for (int pos = 0; pos != market_h; pos += direction.sign()) {
    	estProfit += this.payoffForExchange(pos, direction);
    }
    
    if (firstArrival) {
        firstArrival = false;
      } else {
        double benchDiff = currBenchmark - this.prevBenchmark;
        estProfit += (benchDiff * this.benchmarkReward * this.benchmarkDir);
        this.prev_obs.addProperty("reward", estProfit);
        this.rl_observations.add(prev_obs);
      }
    //double benchDiff = orderSign * (currBenchmark - this.prevBenchmark);
    //estProfit += (benchDiff * this.benchmarkReward * this.benchmarkDir);
    //curr_obs.addProperty("reward", estProfit);
    
    //rl_observations.add(curr_obs);
    
    this.prevBenchmark = currBenchmark;
    this.prev_obs = curr_obs;

    scheduleNextArrival();
  }

  protected final JsonObject stateSpace() {
	JsonObject state = new JsonObject();
	
	double finalEstimate = this.getFinalFundamentalEstiamte();
    state.addProperty("estimate_fundamental", finalEstimate);
    
	ArrayList<Price> bid_vector = market.getBidVector();
    ArrayList<Price> ask_vector = market.getAskVector();
    state.addProperty("bid_depth", bid_vector.size());
    state.addProperty("ask_depth", ask_vector.size());
    if (bid_vector.size() >= this.bookDepth) {
    	bid_vector = new ArrayList<Price>(bid_vector.subList(0, this.bookDepth));
    }
    
    else {
    	Price dummy_bid = Price.of(finalEstimate - 3 * Math.sqrt(this.observationVar));
    	
    	//Super hacky way to force dummy bid to be less than the lowest bid present. 0.3% chance this is ever used though
    	if (bid_vector.size() > 0) {
    		if (dummy_bid.compareTo(bid_vector.get(bid_vector.size()-1)) > 0) {
    			dummy_bid = Price.of(bid_vector.get(bid_vector.size()-1).longValue() - 100);
    		}
    	}
    	for(int i=bid_vector.size(); i<this.bookDepth;i++) {
    		bid_vector.add(i, dummy_bid);
    	}
    }
    if (ask_vector.size() >= this.bookDepth) {
    	ask_vector = new ArrayList<Price>(ask_vector.subList(0, this.bookDepth));
    }
    else {
    	Price dummy_ask = Price.of(finalEstimate + 3 * java.lang.Math.sqrt(this.observationVar));
    	//Super hacky way to force dummy ask to be greater than the highest bid present. 0.3% chance this is ever used though
    	if (ask_vector.size() > 0) {
    		if (dummy_ask.compareTo(ask_vector.get(ask_vector.size()-1)) < 0) {
    			dummy_ask = Price.of(ask_vector.get(ask_vector.size()-1).longValue() + 100);
    		}
    	}
    	for(int i=ask_vector.size(); i<this.bookDepth;i++) {
    		ask_vector.add(i, dummy_ask);
    	}
    }
    state.addProperty("bid_vector", bid_vector.toString());
    state.addProperty("ask_vector", ask_vector.toString());
    
    int num_transactions = market.getCurrentNumTransactions();
    state.addProperty("num_transactions", num_transactions);
    
    int contract_h = this.benchmarkDir * this.benchmarkImpact;
    state.addProperty("contract_holdings", contract_h);
    
    //.getView(this, TimeStamp.ZERO)
    int market_h = market.getHoldings();
    state.addProperty("market_holdings", market_h);
    
    double privateBidBenefit;
    double privateAskBenefit;
    if (Math.abs(market_h + OrderType.BUY.sign()) <= this.maxPosition) {
        privateBidBenefit = OrderType.BUY.sign()
            * this.privateValue.valueForExchange(market_h + OrderType.BUY.sign(), OrderType.BUY);
    }
    else {
    	privateBidBenefit = 0; //Dummy variable
    }
    if (Math.abs(market_h + OrderType.SELL.sign()) <= this.maxPosition) {
        privateAskBenefit = OrderType.SELL.sign()
            * this.privateValue.valueForExchange(market_h + OrderType.SELL.sign(), OrderType.SELL);
    }
    else {
    	privateAskBenefit = 0; //Dummy variable
    }
    state.addProperty("private_bid_benefit", privateBidBenefit);
    state.addProperty("private_ask_benefit", privateAskBenefit);
    
    long timeTilEnd = this.timeHorizon - sim.getCurrentTime().get();
    state.addProperty("time_til_end", timeTilEnd);
    
    return state;
  }
  
  protected final JsonArray stateSpaceArray() {
	JsonArray state = new JsonArray();
	
	double finalEstimate = this.getFinalFundamentalEstiamte();
    state.add(finalEstimate);
    
	ArrayList<Price> bid_vector = market.getBidVector();
    ArrayList<Price> ask_vector = market.getAskVector();
    state.add(bid_vector.size());
    state.add(ask_vector.size());
    if (bid_vector.size() >= this.bookDepth) {
    	bid_vector = new ArrayList<Price>(bid_vector.subList(0, this.bookDepth));
    }
    
    else {
    	Price dummy_bid = Price.of(finalEstimate - 3 * Math.sqrt(this.observationVar));
    	
    	//Super hacky way to force dummy bid to be less than the lowest bid present. 0.3% chance this is ever used though
    	if (bid_vector.size() > 0) {
    		if (dummy_bid.compareTo(bid_vector.get(bid_vector.size()-1)) > 0) {
    			dummy_bid = Price.of(bid_vector.get(bid_vector.size()-1).longValue() - 100);
    		}
    	}
    	for(int i=bid_vector.size(); i<this.bookDepth;i++) {
    		bid_vector.add(i, dummy_bid);
    	}
    }
    if (ask_vector.size() >= this.bookDepth) {
    	ask_vector = new ArrayList<Price>(ask_vector.subList(0, this.bookDepth));
    }
    else {
    	Price dummy_ask = Price.of(finalEstimate + 3 * java.lang.Math.sqrt(this.observationVar));
    	//Super hacky way to force dummy ask to be greater than the highest bid present. 0.3% chance this is ever used though
    	if (ask_vector.size() > 0) {
    		if (dummy_ask.compareTo(ask_vector.get(ask_vector.size()-1)) < 0) {
    			dummy_ask = Price.of(ask_vector.get(ask_vector.size()-1).longValue() + 100);
    		}
    	}
    	for(int i=ask_vector.size(); i<this.bookDepth;i++) {
    		ask_vector.add(i, dummy_ask);
    	}
    }
    
    for(int i = bid_vector.size() - 1; i>=0; i--) {
    	state.add(bid_vector.get(i).doubleValue());
    }
    for(int i = 0; i< ask_vector.size(); i++) {
    	state.add(ask_vector.get(i).doubleValue());
    }
    
    int num_transactions = market.getCurrentNumTransactions();
    state.add(num_transactions);
    
    int contract_h = this.benchmarkDir * this.benchmarkImpact;
    state.add(contract_h);
    
    //.getView(this, TimeStamp.ZERO)
    int market_h = market.getHoldings();
    state.add(market_h);
    
    double privateBidBenefit;
    double privateAskBenefit;
    if (Math.abs(market_h + OrderType.BUY.sign()) <= this.maxPosition) {
        privateBidBenefit = OrderType.BUY.sign()
            * this.privateValue.valueForExchange(market_h + OrderType.BUY.sign(), OrderType.BUY);
    }
    else {
    	privateBidBenefit = 0; //Dummy variable
    }
    if (Math.abs(market_h + OrderType.SELL.sign()) <= this.maxPosition) {
        privateAskBenefit = OrderType.SELL.sign()
            * this.privateValue.valueForExchange(market_h + OrderType.SELL.sign(), OrderType.SELL);
    }
    else {
    	privateAskBenefit = 0; //Dummy variable
    }
    state.add(privateBidBenefit);
    state.add(privateAskBenefit);
    
    long timeTilEnd = this.timeHorizon - sim.getCurrentTime().get();
    state.add(timeTilEnd);
    
    return state;
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
	double estProfit = market.getProfit();
	int market_h = market.getHoldings();
	estProfit += market_h * this.finalFundamental;
    OrderType direction = market_h > 0 ? BUY : SELL;
    for (int pos = 0; pos != market_h; pos += direction.sign()) {
    	estProfit += this.payoffForExchange(pos, direction);
    }

	double finalBenchmark = market.getCurrentBenchmark();
	double benchDiff = finalBenchmark - this.prevBenchmark;
    estProfit += (benchDiff * this.benchmarkReward * this.benchmarkDir);
    
    this.prev_obs.addProperty("reward", estProfit);
    this.rl_observations.add(prev_obs);
  }

  protected double getDesiredSurplus() {
    return shadingDistribution.sample(rand);
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

  protected double getFinalFundamentalEstiamte() {
    return fundamental.getEstimatedFinalFundamental();
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

  @Override
  public final int getId() {
    return id;
  }

  @Override
  public final String toString() {
    return name() + " " + Integer.toUnsignedString(id, 36).toUpperCase();
  }

}