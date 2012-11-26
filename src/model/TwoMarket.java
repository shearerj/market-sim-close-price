package model;

import java.util.ArrayList;
import java.util.Iterator;

import systemmanager.*;

/**
 * TWOMARKET
 * 
 * Class implementing the two-market model of latency arbitrage.
 * 
 * Configurations in the Two-Market Model specify what types of player agents 
 * are allowed. Possible values for the configuration include: "LA", "DUMMY".
 * 
 * @author ewah
 */
public class TwoMarket extends MarketModel {

	private ArrayList<String> permittedMMAgentTypes;
	
	public TwoMarket(ObjectProperties p, SystemData d) {
		super(p, d);
		
		config = p.get(Consts.MODEL_CONFIG_KEY);
		if (!config.equals(Consts.MODEL_CONFIG_NONE) && !config.equals("0")) {
			// Add two CDA markets with default settings
			addMarketPropertyPair("CDA");
			addMarketPropertyPair("CDA");
			
			permittedMMAgentTypes = new ArrayList<String>();
			permittedMMAgentTypes.add(p.get(Consts.MODEL_CONFIG_KEY));
			
		}
	}
	
	@Override
	public String getConfig() {
		return config;
	}
	
	@Override
	public void setAgentPermissions() {
		// Add all single market agents
		permitAllSMAgents();
		
		// Add agent types specified in the spec file
		for (Iterator<String> it = permittedMMAgentTypes.iterator(); it.hasNext(); ) {
			agentIDs.addAll(data.getAgentIDsOfType(it.next()));
		}
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
