package entity;

import data.ObjectProperties;
import data.SystemData;
import systemmanager.*;

/**
 * Abstract class for MarketMakers. Makes it easier to test when 
 * an agent is a market maker.
 * 
 * @author ewah
 */
public abstract class MarketMaker extends SMAgent {

	public MarketMaker(int agentID, int modelID, SystemData d, 
			ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
	}
	
}
