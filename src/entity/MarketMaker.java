package entity;

import systemmanager.*;

/**
 * Abstract class for MarketMakers. Makes it easier to test when 
 * an agent is a market maker.
 * 
 * @author ewah
 */
public abstract class MarketMaker extends SMAgent {

	public MarketMaker(int agentID, int modelID, SystemData d, 
			ObjectProperties p, Log l, int mktID) {
		super(agentID, modelID, d, p, l, mktID);
	}
	
}
