package entity;

import java.util.ArrayList;
import java.util.Collection;

import market.PrivateValue;
import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import event.TimeStamp;

public class MockAgent extends BackgroundAgent {

	public MockAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, PrivateValue pv, RandPlus rand, int tickSize) {
		super(agentID, arrivalTime, model, market, pv, rand, tickSize);
		// TODO Auto-generated constructor stub
	}

	public MockAgent(int agentID, MarketModel model, Market market) {
		this(agentID, new TimeStamp(0), model, market, new PrivateValue(),
				new RandPlus(), 1000);
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		// TODO Auto-generated method stub
		return new ArrayList<Activity>();
	}

}
