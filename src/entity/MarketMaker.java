package entity;

import market.PrivateValue;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;

/**
 * Abstract class for MarketMakers. Makes it easier to test when 
 * an agent is a market maker.
 * 
 * @author ewah
 */
public abstract class MarketMaker extends SMAgent {

	public MarketMaker(int agentID, MarketModel model,
			Market market, RandPlus rand) {
		super(agentID, Consts.START_TIME, model, market, new PrivateValue(), rand);
	}
	
}
