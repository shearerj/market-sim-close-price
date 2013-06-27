package entity;

import java.util.Random;

import data.ObjectProperties;
import data.SystemData;
import event.TimeStamp;
import systemmanager.*;
import utils.RandPlus;

/**
 * Abstract class for background traders. Makes it easier to test when 
 * an agent is in the background role.
 * 
 * @author ewah
 */
public abstract class BackgroundAgent extends SMAgent {

	public BackgroundAgent(int agentID, int modelID, SystemData d, 
			ObjectProperties p) {
		super(agentID, modelID, d, p);
		
		// -- reorg --
		rand = new RandPlus(Long.parseLong(params.get(Agent.RANDSEED_KEY)));
		arrivalTime = new TimeStamp(Long.parseLong(params.get(Agent.ARRIVAL_KEY)));
		// -- reorg --
	}
}
