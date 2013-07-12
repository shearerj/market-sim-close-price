package entity;

import java.util.ArrayList;
import java.util.Collection;

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
			Market market, PrivateValue pv, RandPlus rand) {
		super(agentID, arrivalTime, model, market, pv, rand);
		// TODO Auto-generated constructor stub
	}
	
	public DummyAgent(int agentID, MarketModel model, Market market) {
		this(agentID, new TimeStamp(0), model, market, 
				new PrivateValue(), new RandPlus());
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		// TODO Auto-generated method stub
		return new ArrayList<Activity>();
	}
	
	public Collection<? extends Activity> 
		agentStrategy(TimeStamp currentTime, Price price, int quantity) {
		ArrayList<Activity> bid = new ArrayList<Activity>();
		
		bid.add(new SubmitNMSBid(this, price, quantity, Consts.INF_TIME, currentTime));
		
		return bid;
	}

}
