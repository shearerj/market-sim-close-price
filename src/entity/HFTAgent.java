package entity;

import data.*;
import systemmanager.*;

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
