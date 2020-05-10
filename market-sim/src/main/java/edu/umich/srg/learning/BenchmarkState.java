package edu.umich.srg.learning;

import java.util.ArrayList;

import com.google.gson.JsonArray;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.Keys.ViewBookDepth;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.Keys.BenchmarkDir;
import edu.umich.srg.marketsim.Keys.BenchmarkImpact;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.TransactionDepth;

public class BenchmarkState implements State{
	
	protected final Sim sim;
    private final MarketView market;
	
	private final int bookDepth;
	private final int transactionDepth;
	private final Long timeHorizon;
	private final double observationVar;
	private final int benchmarkImpact;
	private final int benchmarkDir;
	private final int maxPosition;
	
	private int stateSize;
	
	public BenchmarkState(Sim sim, MarketView market, Spec spec) {
		
		this.sim = sim;
	    this.market = market;
		
		this.bookDepth = spec.get(ViewBookDepth.class);
	    this.transactionDepth = spec.get(TransactionDepth.class);
	    this.timeHorizon = spec.get(SimLength.class);
	    this.observationVar = spec.get(FundamentalObservationVariance.class);
	    this.benchmarkImpact = spec.get(BenchmarkImpact.class);
	    this.benchmarkDir = spec.get(BenchmarkDir.class);
	    this.maxPosition = spec.get(MaxPosition.class);
	    
	    this.stateSize = 0;
		
	}
	
	public static BenchmarkState create(Sim sim,  MarketView market, Spec spec) {
		    return new BenchmarkState(sim, market, spec);
		  }
	
	@Override
	public JsonArray getState(double finalEstimate, PrivateValue privateValue) {
		JsonArray state = new JsonArray();
	
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
	    
	    // List of latest transaction prices
	    /*
	    ArrayList<Price> transactions = new ArrayList<>(Arrays.asList(new Price[this.transactionDepth]));
	    Collections.fill(transactions, Price.of(finalEstimate));
	    List<Entry<TimeStamp, Price>> allTransactions = market.getCurrentTransactions();
	    int larger = Math.max(0, allTransactions.size() - this.transactionDepth);
	    int j = 0;
	    for (int i=allTransactions.size()-1; i >= larger; i--) {   	
	    	Price p = allTransactions.get(i).getValue();
	    	transactions.set(j, p);
	    	j++;
	    }
	    for(int i = 0; i < transactions.size();i++) {
	    	state.add(transactions.get(i).doubleValue());
	    }
	    */
	    
	    int num_transactions = market.getCurrentNumTransactions();
	    state.add(num_transactions);
	    
	    int contract_h = this.benchmarkDir * this.benchmarkImpact;
	    state.add(contract_h);
	    
	    int market_h = market.getHoldings();
	    state.add(market_h);
	    
	    double privateBidBenefit;
	    double privateAskBenefit;
	    if (Math.abs(market_h + OrderType.BUY.sign()) <= this.maxPosition) {
	        privateBidBenefit = OrderType.BUY.sign()
	            * privateValue.valueForExchange(market_h + OrderType.BUY.sign(), OrderType.BUY);
	    }
	    else {
	    	privateBidBenefit = 0; //Dummy variable
	    }
	    if (Math.abs(market_h + OrderType.SELL.sign()) <= this.maxPosition) {
	        privateAskBenefit = OrderType.SELL.sign()
	            * privateValue.valueForExchange(market_h + OrderType.SELL.sign(), OrderType.SELL);
	    }
	    else {
	    	privateAskBenefit = 0; //Dummy variable
	    }
	    state.add(privateBidBenefit);
	    state.add(privateAskBenefit);
	    
	    long timeTilEnd = this.timeHorizon - sim.getCurrentTime().get();
	    state.add(timeTilEnd);
	    
	    this.stateSize = state.size();
	    
	    return state;
	  }
	
	@Override
	public int getStateSize() {
		return this.stateSize + 1; //add 1 for side as feature
	}

}