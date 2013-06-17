package entity;

import data.*;
import systemmanager.*;

/**
 * Abstract class for high-frequency traders.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	public HFTAgent(int agentID, int modelID, SystemData d, 
			ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
	}
}
