package entity.agent;

import entity.market.Market;
import event.TimeStamp;
import model.MarketModel;
import utils.RandPlus;

/**
 * Abstract class for MarketMakers. Makes it easier to test when an agent is a market maker.
 * 
 * @author ewah
 */
// TODO Currently MarketMakers don't ever schedule a liquidate. Not sure exactly when this should
// happen.
public abstract class MarketMaker extends SMAgent {

	private static final long serialVersionUID = -782740037969385370L;

	public MarketMaker(MarketModel model, Market market,
			RandPlus rand, int tickSize) {
		super(TimeStamp.ZERO, model, market, new PrivateValue(),
				rand, tickSize);
	}

}
