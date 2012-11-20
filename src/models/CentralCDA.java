package models;

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

	public CentralCDA(ObjectProperties p, SystemData d) {
		super(p, d);
		
		String type = p.get(Consts.MODEL_TYPE_KEY);
		if (!type.equals(Consts.MODEL_TYPE_NONE) && !type.equals("0")) {
			addMarketPropertyPair("CDA");
		}
	}
	
	@Override
	public void setAgentPermissions() {
		permitAllSMAgents();
	}
}
