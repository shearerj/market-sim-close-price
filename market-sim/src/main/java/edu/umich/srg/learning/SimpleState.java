package edu.umich.srg.learning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.Keys.ViewBookDepth;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.Keys.StateSpaceFlags;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.TransactionDepth;
import edu.umich.srg.marketsim.Keys.OmegaDepth;

public class SimpleState implements State{
	
	protected final Sim sim;
    protected final MarketView market;
	
	private final int bookDepth;
	private final int transactionDepth;
	private final int omegaDepth;
	private final Long timeHorizon;
	private final double observationVar;
	private final int maxPosition;
	
	protected final JsonObject stateFlags;
	
	protected int stateSize;
	
	protected ArrayList<Price> bid_vector;
	protected ArrayList<Price> ask_vector;
	protected ArrayList<Price> transactions;
	
	protected MatrixLibrary mtxLib;
	
	public SimpleState(Sim sim, MarketView market, Spec spec) {
		
		this.sim = sim;
	    this.market = market;
		
		this.bookDepth = spec.get(ViewBookDepth.class);
	    this.transactionDepth = spec.get(TransactionDepth.class);
	    this.omegaDepth = spec.get(OmegaDepth.class);
	    this.timeHorizon = spec.get(SimLength.class);
	    this.observationVar = spec.get(FundamentalObservationVariance.class);
	    this.maxPosition = spec.get(MaxPosition.class);
	    
	    this.stateSize = 0;
	    
	    this.bid_vector = new ArrayList<Price>();
	    this.ask_vector = new ArrayList<Price>();
	    this.transactions = new ArrayList<Price>();
	    
	    this.mtxLib = new MatrixLibrary();
	    
	    String stateFlagString = spec.get(StateSpaceFlags.class).iterator().next();
	    JsonParser parser = new JsonParser();
	    this.stateFlags = (JsonObject) parser.parse(stateFlagString);
		
	}
	
	public static SimpleState create(Sim sim,  MarketView market, Spec spec) {
		    return new SimpleState(sim, market, spec);
		  }
	
	@Override
	public JsonArray getState(double finalEstimate, int side, PrivateValue privateValue) {
		JsonArray state = new JsonArray();
	
		if(stateFlags.get("finalFundamentalEstimate").getAsBoolean()) {
			//state.add(Math.log(finalEstimate));
			state.add(finalEstimate);
		}
	    
		if(stateFlags.get("side").getAsBoolean()) {
			state.add(side);
		}
	    
	    this.bid_vector = market.getBidVector();
	    this.ask_vector = market.getAskVector();
	    
	    if(stateFlags.get("bidSize").getAsBoolean()) {
	    	state.add(bid_vector.size());
		}
	    if(stateFlags.get("askSize").getAsBoolean()) {
	    	state.add(ask_vector.size());
		}
	    
	    if(stateFlags.get("bidVector").getAsBoolean()) {
	    	this.getBidVector(market, finalEstimate);
		    for(int i = this.bid_vector.size() - 1; i>=0; i--) {
		    	//state.add(this.bid_vector.get(i).doubleValue());
		    	//state.add(Math.log(this.bid_vector.get(i).doubleValue()));
		    	state.add(finalEstimate - this.bid_vector.get(i).doubleValue());
		    }	
		}
	    
	    if(stateFlags.get("askVector").getAsBoolean()) {
	    	this.getAskVector(market, finalEstimate);
		    for(int i = 0; i< this.ask_vector.size(); i++) {
		    	//state.add(this.ask_vector.get(i).doubleValue());
		    	//state.add(Math.log(this.ask_vector.get(i).doubleValue()));
		    	state.add(finalEstimate - this.ask_vector.get(i).doubleValue());
		    }
		}
	    
	    if(stateFlags.get("transactionHistory").getAsBoolean()) {
	    	this.getTransactionHistory(finalEstimate);
		    for(int i = 0; i < transactions.size();i++) {
		    	//state.add(transactions.get(i).doubleValue());
		    	//state.add(Math.log(transactions.get(i).doubleValue()));
		    	state.add(finalEstimate - transactions.get(i).doubleValue());
		    }
		}
	    
	    int market_h = market.getHoldings();
	    if(stateFlags.get("marketHoldings").getAsBoolean()) {
	    	state.add(market_h);
		}
	    
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
	    
	    if(stateFlags.get("privateBid").getAsBoolean()) {
	    	state.add(privateBidBenefit);
	    	//state.add(privateBidBenefit/1000);
		}
	    
	    if(stateFlags.get("privateAsk").getAsBoolean()) {
	    	state.add(privateAskBenefit);
	    	//state.add(privateAskBenefit/1000);
		}
	    
	    if(stateFlags.get("omegaRatio").getAsBoolean()) {
	    	double omega_bid = this.omegaRatio(finalEstimate + privateBidBenefit);
	    	double omega_ask = this.omegaRatio(finalEstimate + privateAskBenefit);
	    	state.add(omega_bid);
		    state.add(omega_ask);
	    	/*
	    	if ((omega_bid == 0) && (omega_ask == 0)) {
	    		state.add(omega_bid);
			    state.add(omega_ask);
	    	}
	    	else {
	    		state.add(omega_bid/(omega_bid + omega_ask));
			    state.add(omega_ask/(omega_bid + omega_ask));
	    	}
	    	*/
		}
	    
	    if(stateFlags.get("timeTilEnd").getAsBoolean()) {
	    	long timeTilEnd = this.timeHorizon - sim.getCurrentTime().get();
	    	state.add(timeTilEnd);
		    //state.add(((double)timeTilEnd)/((double)this.timeHorizon));
		}
	    
	    this.stateSize = state.size();
	    
	    return state;
	  }
	
	public JsonArray getNormState(JsonArray state) {
		double[][] stateMtx = mtxLib.jsonToVector(state, state.size());
		stateMtx = mtxLib.norm(stateMtx, state.size());
		return mtxLib.vectorToJson(stateMtx, state.size());
	}
	
	protected void getBidVector(MarketView market, double finalEstimate) {
	    
	    if (this.bid_vector.size() >= this.bookDepth) {
	    	this.bid_vector = new ArrayList<Price>(this.bid_vector.subList(0, this.bookDepth));
	    }
	    
	    else {
	    	Price dummy_bid = Price.of(finalEstimate - 3 * Math.sqrt(this.observationVar));
	    	
	    	//Super hacky way to force dummy bid to be less than the lowest bid present. 0.3% chance this is ever used though
	    	if (this.bid_vector.size() > 0) {
	    		if (dummy_bid.compareTo(this.bid_vector.get(this.bid_vector.size()-1)) > 0) {
	    			dummy_bid = Price.of(this.bid_vector.get(this.bid_vector.size()-1).longValue() - 100);
	    		}
	    	}
	    	for(int i=this.bid_vector.size(); i<this.bookDepth;i++) {
	    		this.bid_vector.add(i, dummy_bid);
	    	}
	    }
	}
	
	protected void getAskVector(MarketView market, double finalEstimate) {
	    if (this.ask_vector.size() >= this.bookDepth) {
	    	this.ask_vector = new ArrayList<Price>(this.ask_vector.subList(0, this.bookDepth));
	    }
	    else {
	    	Price dummy_ask = Price.of(finalEstimate + 3 * java.lang.Math.sqrt(this.observationVar));
	    	//Super hacky way to force dummy ask to be greater than the highest bid present. 0.3% chance this is ever used though
	    	if (this.ask_vector.size() > 0) {
	    		if (dummy_ask.compareTo(this.ask_vector.get(this.ask_vector.size()-1)) < 0) {
	    			dummy_ask = Price.of(this.ask_vector.get(this.ask_vector.size()-1).longValue() + 100);
	    		}
	    	}
	    	for(int i=this.ask_vector.size(); i<this.bookDepth;i++) {
	    		this.ask_vector.add(i, dummy_ask);
	    	}
	    }
	}
	
	protected void getTransactionHistory(double finalEstimate) {
		this.transactions = new ArrayList<>(Arrays.asList(new Price[this.transactionDepth]));
	    Collections.fill(this.transactions, Price.of(finalEstimate));
	    List<Entry<TimeStamp, Price>> allTransactions = market.getCurrentTransactions();
	    int larger = Math.max(0, allTransactions.size() - this.transactionDepth);
	    int j = 0;
	    for (int i=allTransactions.size()-1; i >= larger; i--) {   	
	    	Price p = allTransactions.get(i).getValue();
	    	this.transactions.set(j, p);
	    	j++;
	    }
	}
	
	protected double omegaRatio(final double cutoff) {
	    	final int minLength = 2;
	    	List<Entry<TimeStamp, Price>> allTransactions = market.getCurrentTransactions();
	    	if (allTransactions.size() <= minLength) {
	    		// too little history to have a meaningful
	    		// omega ratio. return default placeholder of 1.0.
	    		return 1.0;
	    	}
	    	if (allTransactions.size() > this.omegaDepth) {
	    		allTransactions.subList(allTransactions.size() - this.omegaDepth, allTransactions.size());
	    	}
	    	// let num = sum over values >= cutoff of (value - cutoff).
	    	// let denom = sum over values < cutoff of (cutoff - value).
	    	// return num / denom.
	    	double num = 0.0;
	    	double denom = 0.0;
	    	for(Entry<TimeStamp, Price> trade: allTransactions) {
	    		if (trade.getValue().doubleValue() < cutoff) {
	    			denom += cutoff - trade.getValue().doubleValue();
	    		} else {
	    			num += trade.getValue().doubleValue() - cutoff;
	    		}
	    	}
	    	if (denom == 0) {
	    		return num;
	    	}
	    	return num * 1.0 / denom;
	    }
	
	@Override
	public int getStateSize() {
		return this.stateSize;
	}

}