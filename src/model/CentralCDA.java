package model;

import java.util.Iterator;

import systemmanager.*;

/**
 * CENTRALCDA
 * 
 * Class implementing the centralized (unfragmented) CDA market. Uses default
 * properties as given in Consts class.
 * 
 * @author ewah
 */
public class CentralCDA extends MarketModel {

	public CentralCDA(int modelID, ObjectProperties p, SystemData d) {
		super(modelID, p, d);
		
		config = p.get(Consts.MODEL_CONFIG_KEY);
		if (!config.equals(Consts.MODEL_CONFIG_NONE) && !config.equals("0")) {
			addMarketPropertyPair("CDA");
			
			addAllEnvironmentAgents();
			//setSingleAgentType("MARKETMAKER");
		}
	}
	
	@Override
	public String getConfig() {
		return "";
	}
}
