package entity.agent;

import model.MarketModel;
import utils.RandPlus;
import entity.market.Market;
import event.TimeStamp;

/**
 * Abstract class for background traders. Makes it easier to test when 
 * an agent is in the background role.
 * 
 * @author ewah
 */
public abstract class BackgroundAgent extends SMAgent {
	
	private static final long serialVersionUID = 7742389103679854398L;

	public BackgroundAgent(TimeStamp arrivalTime, MarketModel model, 
			Market market, PrivateValue pv, RandPlus rand, int tickSize) {
		super(arrivalTime, model, market, pv, rand, tickSize);
	}
	
}
