package model;

import java.util.ArrayList;
import java.util.Iterator;

import systemmanager.*;

/**
 * TWOMARKET
 * 
 * Class implementing the two-market model of latency arbitrage.
 * 
 * Configurations in the Two-Market Model specify what types of strategies
 * are allowed. Possible values for the configuration include: "LA", "DUMMY".
 * 
 * @author ewah
 */
public class TwoMarket extends MarketModel {
	
	public TwoMarket(int modelID, ObjectProperties p, SystemData d) {
		super(modelID,p, d);
		
		config = p.get(Consts.MODEL_CONFIG_KEY);
		if (!config.equals(Consts.MODEL_CONFIG_NONE) && !config.equals("0")) {
			// Add two CDA markets with default settings
			addMarketPropertyPair("CDA");
			addMarketPropertyPair("CDA");
			
			// set the permitted HFT agent type, set all other ones to 0
			String type = p.get(Consts.MODEL_CONFIG_KEY);
			numAgentType.put(type, data.getNumAgentType(type));
			
			addAllSMAgents();
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
}
