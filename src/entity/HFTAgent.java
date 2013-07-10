package entity;

import market.PrivateValue;
import model.MarketModel;
import utils.RandPlus;
import event.TimeStamp;

/**
 * Abstract class for high-frequency traders.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	public HFTAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			int sleepTime, double sleepVar, RandPlus rand) {
		super(agentID, arrivalTime, model, new PrivateValue(), sleepTime, sleepVar, rand);
	}
}