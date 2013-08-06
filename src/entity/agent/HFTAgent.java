package entity.agent;

import model.MarketModel;
import utils.RandPlus;
import entity.market.PrivateValue;
import event.TimeStamp;

/**
 * Abstract class for high-frequency traders.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	public HFTAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			RandPlus rand, int tickSize) {
		super(agentID, arrivalTime, model, new PrivateValue(), rand, tickSize);
	}
	
}
