package entity.agent;

import model.MarketModel;
import utils.RandPlus;
import event.TimeStamp;

/**
 * Abstract class for high-frequency traders.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	private static final long serialVersionUID = -1483633963238206201L;

	public HFTAgent(TimeStamp arrivalTime, MarketModel model,
			RandPlus rand, int tickSize) {
		super(arrivalTime, model, new PrivateValue(), rand, tickSize);
	}
	
}
