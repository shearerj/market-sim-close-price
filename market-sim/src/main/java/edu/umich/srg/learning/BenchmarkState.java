package edu.umich.srg.learning;

import com.google.gson.JsonArray;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.Keys.BenchmarkDir;
import edu.umich.srg.marketsim.Keys.BenchmarkImpact;

public class BenchmarkState extends SimpleState{
	
	private final int benchmarkImpact;
	private final int benchmarkDir;
	
	public BenchmarkState(Sim sim, MarketView market, Spec spec) {
		super(sim,market,spec);
	    this.benchmarkImpact = spec.get(BenchmarkImpact.class);
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
	    	int contract_h = this.benchmarkDir * this.benchmarkImpact;
		    state.add(Math.log(contract_h));
		}
	    
	    this.stateSize = state.size();
	    
	    return state;
	  }

}