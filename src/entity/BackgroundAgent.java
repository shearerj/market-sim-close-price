package entity;

import model.MarketModel;
import utils.RandPlus;
import event.TimeStamp;

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
	
}
