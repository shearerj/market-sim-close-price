package model;

import java.util.Map;

import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import data.EntityProperties;
import entity.CDAMarket;

/**
 * TWOMARKET
 * 
 * Class implementing the two-market model of latency arbitrage.
 * 
 * Configurations in the Two-Market Model specify what types of strategies
 * are allowed. Possible values for the configuration include:
 * 
 * 		"LA:<strategy_string>"
 * 		"DUMMY"
 * 
 * @author ewah
 */
public class TwoMarket extends MarketModel {
	
	public TwoMarket(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			EntityProperties modelProps, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, rand);
	}
	
	@Override
	@Deprecated
	public String getConfig() {
		return config;
	}
	
	@Deprecated
	public int getAlternateMarket(int mainMarketID) {
		if (marketIDs.contains(mainMarketID)) {
			if (marketIDs.get(0) == mainMarketID) {
				return marketIDs.get(1);
			} else {
				return marketIDs.get(0);
			}
		}
		return 0;
	}


	@Override
	protected void setupMarkets(EntityProperties modelProps) {
		markets.add(new CDAMarket(1, this));
		markets.add(new CDAMarket(2, this));
	}

	@Override
	protected void setupAgents(EntityProperties modelProps,
			Map<AgentProperties, Integer> agentProps) {
		super.setupAgents(modelProps, agentProps);
		// Look at model parameters and set up LA
	}

}
