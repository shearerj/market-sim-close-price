package model;

import java.util.Map;

import data.*;
import entity.CDAMarket;
import systemmanager.*;
import systemmanager.Consts.AgentType;
import utils.RandPlus;

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
			ObjectProperties modelProps, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, rand);
	}
	
	public TwoMarket(int modelID, ObjectProperties p, SystemData d) {
		super(modelID,p, d);
		
		config = p.getAsString(Consts.MODEL_CONFIG_KEY);
		if (!config.equals(Consts.MODEL_CONFIG_NONE) && !config.equals("0")) {
			// Add two CDA markets with default settings
			addMarketPropertyPair(Consts.MarketType.CDA);
			addMarketPropertyPair(Consts.MarketType.CDA);
			
			// Check that config is not blank
			if (!config.equals("")) {
				// split on colon
				String[] as = config.split("[:]+");
				AgentType agType = AgentType.valueOf(as[0]);
				if (!Consts.MM_AGENT.contains(agType)) {
					System.err.println(this.getClass().getSimpleName() + "::parseConfigs: " +
									"model configuration " + config + " incorrect");
					System.exit(1);
				}
				if (as.length == 2) {
					ObjectProperties op = SimulationSpec.getAgentProperties(agType, as[1]);
					addAgentPropertyPair(agType, op);
				} else if (as.length == 1) {
					ObjectProperties op = SimulationSpec.getAgentProperties(agType, "");
					addAgentPropertyPair(agType, op);
				}
			}
		}
	}
	
	
	@Override
	public String getConfig() {
		return config;
	}

	
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
	protected void setupMarkets(ObjectProperties modelProps) {
		markets.add(new CDAMarket(1, this));
		markets.add(new CDAMarket(2, this));
	}


	@Override
	protected void setupModelAgents(ObjectProperties modelProps) {
		// TODO check for LA and if so add it with proper properties
	}
}
