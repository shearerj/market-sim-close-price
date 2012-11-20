package models;

import java.util.ArrayList;
import java.util.Iterator;

import systemmanager.*;

/**
 * TWOMARKET
 * 
 * Class implementing the two-market model of latency arbitrage.
 * 
 * Type in the Two-Market model specifies what non-SM agent is allowed. Possible
 * values for the model type include: "LA", "DUMMY".
 * 
 * @author ewah
 */
public class TwoMarket extends MarketModel {

	private ArrayList<String> permittedMMAgentTypes;
	
	public TwoMarket(ObjectProperties p, SystemData d) {
		super(p, d);
		
		String type = p.get(Consts.MODEL_TYPE_KEY);
		if (!type.equals(Consts.MODEL_TYPE_NONE) && !type.equals("0")) {
			// Add two CDA markets with default settings
			addMarketPropertyPair("CDA");
			addMarketPropertyPair("CDA");
			
			permittedMMAgentTypes = new ArrayList<String>();
			permittedMMAgentTypes.add(p.get(Consts.MODEL_TYPE_KEY));
			
		}
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
