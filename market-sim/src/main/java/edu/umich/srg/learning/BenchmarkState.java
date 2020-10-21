package edu.umich.srg.learning;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.Keys.BenchmarkDir;
import edu.umich.srg.marketsim.Keys.ContractHoldings;;

public class BenchmarkState extends SimpleState{
	
	private final double contractHoldings;
	private final int benchmarkDir;
	
	public BenchmarkState(Sim sim, MarketView market, Spec spec) {
		super(sim,market,spec);
	    this.contractHoldings = spec.get(ContractHoldings.class);
	    this.benchmarkDir = spec.get(BenchmarkDir.class);
		
	}
	
	public static BenchmarkState create(Sim sim,  MarketView market, Spec spec) {
		    return new BenchmarkState(sim, market, spec);
		  }
	
	@Override
	public JsonArray getState(double finalEstimate, int side, PrivateValue privateValue) {
		JsonArray state = super.getState(finalEstimate, side, privateValue);
	    
		if(stateFlags.get("numTransactions").getAsBoolean()) {
			int num_transactions = market.getCurrentNumTransactions();
		    state.add(num_transactions);
		}
	    
	    if(stateFlags.get("contractHoldings").getAsBoolean()) {
	    	double contract_h = this.benchmarkDir * this.contractHoldings;
	    	state.add(contract_h);
		    //state.add(Math.log(contract_h));
		}
	    
	    this.stateSize = state.size();
	    
	    return state;
	  }
	
	@Override
	public JsonObject getStateDict(double finalEstimate, int side, PrivateValue privateValue) {
		JsonObject state = super.getStateDict(finalEstimate, side, privateValue);
	    
		if(stateFlags.get("numTransactions").getAsBoolean()) {
			int num_transactions = market.getCurrentNumTransactions();
		    state.addProperty("numTransactions",num_transactions);
		}
	    
	    if(stateFlags.get("contractHoldings").getAsBoolean()) {
	    	double contract_h = this.benchmarkDir * this.contractHoldings;
	    	state.addProperty("contractHoldings",contract_h);
		    //state.add(Math.log(contract_h));
		}
	    
	    this.stateSize = state.size();
	    
	    return state;
	  }

}