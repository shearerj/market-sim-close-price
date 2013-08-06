package entity;

import entity.market.Market;
import entity.market.PrivateValue;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;

/**
 * Abstract class for MarketMakers. Makes it easier to test when an agent is a market maker.
 * 
 * @author ewah
 */
// TODO Currently MarketMakers don't ever schedule a liquidate. Not sure exactly when this should
// happen.
public abstract class MarketMaker extends SMAgent {
	public MarketMaker(int agentID, MarketModel model, Market market,
			RandPlus rand, int tickSize) {
		super(agentID, Consts.START_TIME, model, market, new PrivateValue(),
				rand, tickSize);
	}

}
