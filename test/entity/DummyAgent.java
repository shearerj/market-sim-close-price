package entity;

import java.util.ArrayList;
import java.util.Collection;

import market.PQBid;
import market.PQPoint;
import market.Price;
import market.PrivateValue;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.SubmitNMSBid;
import event.TimeStamp;

public class DummyAgent extends BackgroundAgent {

	public DummyAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, PrivateValue pv, RandPlus rand, int tickSize) {
		super(agentID, arrivalTime, model, market, pv, rand, tickSize);
		// TODO Auto-generated constructor stub
	}

	public DummyAgent(int agentID, MarketModel model, Market market) {
		this(agentID, new TimeStamp(0), model, market, new PrivateValue(),
				new RandPlus(), 1000);
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		// TODO Auto-generated method stub
		return new ArrayList<Activity>();
	}

	/**
	 * Submits a PQBid according to input market directly to the market
	 * 
	 * @param currentTime
	 * @param price
	 * @param quantity
	 * @return PQBid that was submitted to the market
	 */
	public PQBid agentStrategy(TimeStamp currentTime, Price price, int quantity) {
		PQBid bid = new PQBid(this, market, currentTime);
		bid.addPoint(new PQPoint(quantity, price));
		market.addBid(bid, currentTime);
		return bid;
	}

}
