package entity;

import data.ObjectProperties;
import data.SystemData;

/**
 * Abstract class for high-frequency traders.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	public HFTAgent(int agentID, int modelID, SystemData d, ObjectProperties p) {
		super(agentID, modelID, d, p);
	}
}
