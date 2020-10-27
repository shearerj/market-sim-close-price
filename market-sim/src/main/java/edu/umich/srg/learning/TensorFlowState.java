package edu.umich.srg.learning;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.Keys.MaxVectorDepth;
import edu.umich.srg.marketsim.Keys.OmegaDepth;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.BenchmarkDir;
import edu.umich.srg.marketsim.Keys.CommunicationLatency;
import edu.umich.srg.marketsim.Keys.ContractHoldings;

public class TensorFlowState implements State{
	
	protected final Sim sim;
    protected final MarketView market;
    
    private final int omegaDepth;
	private final int maxVectorDepth;
	private final Long timeHorizon;
	private final double observationVar;
	private final long latency;
	private final double contractHoldings;
	private final int benchmarkDir;
	private final int maxPosition;
	
	protected int stateSize;
	
	protected ArrayList<Price> bid_vector;
	protected ArrayList<Price> ask_vector;
	protected ArrayList<Price> transactions;
	
	protected MatrixLibrary mtxLib;
	
	public TensorFlowState(Sim sim, MarketView market, Spec spec) {
		
		this.sim = sim;
	    this.market = market;
		
	    this.omegaDepth = spec.get(OmegaDepth.class);
	    this.maxVectorDepth = spec.get(MaxVectorDepth.class);
	    this.timeHorizon = spec.get(SimLength.class);
	    this.observationVar = spec.get(FundamentalObservationVariance.class);
	    this.latency = spec.get(CommunicationLatency.class);
	    this.contractHoldings = spec.get(ContractHoldings.class);
	    this.benchmarkDir = spec.get(BenchmarkDir.class);
	    this.maxPosition = spec.get(MaxPosition.class);
	    
	    this.stateSize = 0;
	    
	    this.bid_vector = new ArrayList<Price>();
	    this.ask_vector = new ArrayList<Price>();
	    this.transactions = new ArrayList<Price>();
	    
	    this.mtxLib = new MatrixLibrary();
		
	}
	
	public static TensorFlowState create(Sim sim,  MarketView market, Spec spec) {
		    return new TensorFlowState(sim, market, spec);
		  }
	
	@Override
	public JsonArray getState(double finalEstimate, int side, PrivateValue privateValue) {
		JsonArray state = new JsonArray();
	
		state.add(finalEstimate);
	    
		state.add(side);
		
		state.add(this.latency);
	    
	    this.bid_vector = market.getBidVector();
	    this.ask_vector = market.getAskVector();
	    state.add(bid_vector.size());
	    state.add(ask_vector.size());
	    
	    int num_transactions = market.getCurrentNumTransactions();
	    state.add(num_transactions);
	    
	    this.getBidVector(market, finalEstimate);
	    for(int i = this.bid_vector.size() - 1; i>=0; i--) {
	    	state.add(this.bid_vector.get(i).doubleValue() - finalEstimate);
	    }
	    
	    this.getAskVector(market, finalEstimate);
	    for(int i = 0; i< this.ask_vector.size(); i++) {
	    	state.add(this.ask_vector.get(i).doubleValue() - finalEstimate);
	    }
	    
	    state.add(this.ask_vector.get(0).doubleValue() - this.bid_vector.get(0).doubleValue());
	    
	    this.getTransactionHistory(finalEstimate);
	    for(int i = 0; i < transactions.size();i++) {
	    	state.add(transactions.get(i).doubleValue() - finalEstimate);
	    }
	    
	    int market_h = market.getHoldings();
	    state.add(market_h);
	    
	    double contract_h = this.benchmarkDir * this.contractHoldings;
    	state.add(contract_h);
    	
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
	    
	    double omega_bid = this.omegaRatio(finalEstimate + privateBidBenefit);
    	state.add(omega_bid);
	    
	    double omega_ask = this.omegaRatio(finalEstimate + privateAskBenefit);
	    state.add(omega_ask);
	    
	    long timeTilEnd = this.timeHorizon - sim.getCurrentTime().get();
    	state.add(timeTilEnd);
	    
	    this.stateSize = state.size();
	    
	    return state;
	  }
	
	
	@Override
	public JsonObject getStateDict(double finalEstimate, int side, PrivateValue privateValue) {
		JsonObject state = new JsonObject();
	
		state.addProperty("finalFundamentalEstimate", finalEstimate);
	    
		state.addProperty("side", side);
		
		state.addProperty("latency", this.latency);
	    
	    this.bid_vector = market.getBidVector();
	    this.ask_vector = market.getAskVector();
	    state.addProperty("bidSize", bid_vector.size());
	    state.addProperty("askSize", ask_vector.size());
	    
	    int num_transactions = market.getCurrentNumTransactions();
	    state.addProperty("numTransactions",num_transactions);
	    
	    this.getBidVector(market, finalEstimate);
    	ArrayList<Double> bid_vec_double = new ArrayList<Double>();
    	JsonArray bid_vec = new JsonArray();
    	for(int i = this.bid_vector.size() - 1; i>=0; i--) {
    		bid_vec_double.add(this.bid_vector.get(i).doubleValue() - finalEstimate);
    		bid_vec.add(this.bid_vector.get(i).doubleValue() - finalEstimate);
	    }	
    	//state.addProperty("bidVector", bid_vec_double.toString());
    	state.add("bidVector", bid_vec);
	    
    	this.getAskVector(market, finalEstimate);
    	ArrayList<Double> ask_vec_double = new ArrayList<Double>();
    	JsonArray ask_vec = new JsonArray();
    	for(int i = 0; i< this.ask_vector.size(); i++) {
    		ask_vec_double.add(this.ask_vector.get(i).doubleValue() - finalEstimate);
    		ask_vec.add(this.ask_vector.get(i).doubleValue() - finalEstimate);
	    }
    	//state.addProperty("askVector", ask_vec_double.toString());
    	state.add("askVector", ask_vec);
	    
    	state.addProperty("spread", this.ask_vector.get(0).doubleValue() - this.bid_vector.get(0).doubleValue());
	    
    	this.getTransactionHistory(finalEstimate);
    	ArrayList<Double> trans_double = new ArrayList<Double>();
    	JsonArray trans_json = new JsonArray();
    	for(int i = 0; i < transactions.size();i++) {
	    	trans_double.add(transactions.get(i).doubleValue() - finalEstimate);
	    	trans_json.add(transactions.get(i).doubleValue() - finalEstimate);
	    }
    	//state.addProperty("transactionHistory", trans_double.toString());
    	state.add("transactionHistory", trans_json);
    	
    	int market_h = market.getHoldings();
	    state.addProperty("marketHoldings",market_h);
	    
	    double contract_h = this.benchmarkDir * this.contractHoldings;
    	state.addProperty("contractHoldings",contract_h);
    	
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
	    
	    state.addProperty("privateBid",privateBidBenefit);
	    
	    state.addProperty("privateAsk",privateAskBenefit);
	    
	    double omega_bid = this.omegaRatio(finalEstimate + privateBidBenefit);
    	state.addProperty("omegaRatioBid",omega_bid);
	    
    	double omega_ask = this.omegaRatio(finalEstimate + privateAskBenefit);
	    state.addProperty("omegaRatioAsk",omega_ask);
	    
	    long timeTilEnd = this.timeHorizon - sim.getCurrentTime().get();
    	state.addProperty("timeTilEnd",timeTilEnd);
	    
	    this.stateSize = state.size();
	    
	    return state;
	  }
	
	public JsonArray getNormState(JsonArray state) {
		double[][] stateMtx = mtxLib.jsonToVector(state, state.size());
		stateMtx = mtxLib.norm(stateMtx, state.size());
		return mtxLib.vectorToJson(stateMtx, state.size());
	}
	
	protected void getBidVector(MarketView market, double finalEstimate) {
	    
	    if (this.bid_vector.size() >= this.maxVectorDepth) {
	    	this.bid_vector = new ArrayList<Price>(this.bid_vector.subList(0, this.maxVectorDepth));
	    }
	    
	    else {
	    	Price dummy_bid = Price.of(finalEstimate - 3 * Math.sqrt(this.observationVar));
	    	
	    	//Super hacky way to force dummy bid to be less than the lowest bid present. 0.3% chance this is ever used though
	    	if (this.bid_vector.size() > 0) {
	    		if (dummy_bid.compareTo(this.bid_vector.get(this.bid_vector.size()-1)) > 0) {
	    			dummy_bid = Price.of(this.bid_vector.get(this.bid_vector.size()-1).longValue() - 100);
	    		}
	    	}
	    	for(int i=this.bid_vector.size(); i<this.maxVectorDepth;i++) {
	    		this.bid_vector.add(i, dummy_bid);
	    	}
	    }
	}
	
	protected void getAskVector(MarketView market, double finalEstimate) {
	    if (this.ask_vector.size() >= this.maxVectorDepth) {
	    	this.ask_vector = new ArrayList<Price>(this.ask_vector.subList(0, this.maxVectorDepth));
	    }
	    else {
	    	Price dummy_ask = Price.of(finalEstimate + 3 * java.lang.Math.sqrt(this.observationVar));
	    	//Super hacky way to force dummy ask to be greater than the highest bid present. 0.3% chance this is ever used though
	    	if (this.ask_vector.size() > 0) {
	    		if (dummy_ask.compareTo(this.ask_vector.get(this.ask_vector.size()-1)) < 0) {
	    			dummy_ask = Price.of(this.ask_vector.get(this.ask_vector.size()-1).longValue() + 100);
	    		}
	    	}
	    	for(int i=this.ask_vector.size(); i<this.maxVectorDepth;i++) {
	    		this.ask_vector.add(i, dummy_ask);
	    	}
	    }
	}
	
	protected void getTransactionHistory(double finalEstimate) {
		this.transactions = new ArrayList<>(Arrays.asList(new Price[this.maxVectorDepth]));
	    Collections.fill(this.transactions, Price.of(finalEstimate));
	    List<Entry<TimeStamp, Price>> allTransactions = market.getCurrentTransactions();
	    int larger = Math.max(0, allTransactions.size() - this.maxVectorDepth);
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