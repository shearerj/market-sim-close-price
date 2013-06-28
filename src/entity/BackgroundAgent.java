package entity;

import model.MarketModel;
import data.ObjectProperties;
import data.SystemData;
import event.TimeStamp;
import utils.RandPlus;

/**
 * Abstract class for background traders. Makes it easier to test when 
 * an agent is in the background role.
 * 
 * @author ewah
 */
public abstract class BackgroundAgent extends SMAgent {
	
	public BackgroundAgent(int agentID, TimeStamp arrivalTime, MarketModel model, Market market, RandPlus rand) {
		super(agentID, arrivalTime, model, market, rand);
	}

	public BackgroundAgent(int agentID, int modelID, SystemData d, 
			ObjectProperties p) {
		super(agentID, modelID, d, p);
		
		// -- reorg --
//		rand = new RandPlus(params.getAsLong(Agent.RANDSEED_KEY));
		arrivalTime = new TimeStamp(params.getAsLong(Agent.ARRIVAL_KEY));
		// -- reorg --
	}
}
