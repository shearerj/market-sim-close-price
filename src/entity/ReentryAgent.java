package entity;


import java.util.ArrayList;

import data.ArrivalTime;
import market.PrivateValue;
import model.MarketModel;
import utils.RandPlus;
import event.TimeStamp;


/**
 * Establishes a common implementation for determining re-entry time
 * @author drhurd
 *
 */
public abstract class ReentryAgent extends BackgroundAgent {

	protected ArrivalTime reentry; // re-entry times
	
	public ReentryAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, PrivateValue pv, RandPlus rand, int reentryRate, 
			int tickSize) {
		super(agentID, arrivalTime, model, market, pv, rand, tickSize);
	
		
		//Creating the reentry object
		this.reentry = new ArrivalTime(arrivalTime, reentryRate, rand);
	}
	
	public TimeStamp getNextReentryTime() {
		return reentry.next();
	}
	
	public ArrayList<TimeStamp> getReentryTimes() {
		return reentry.getArrivalTimes();
	}

}
