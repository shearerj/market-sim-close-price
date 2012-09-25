package entity;

import event.*;
import activity.*;
import systemmanager.*;

/**
 * Single market (SM) agent, whose agent strategy is called once for each market.
 * 
 * @author ewah
 */
public abstract class SMAgent extends Agent {

	public SMAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
	}
	
	/**
	 * Function specifying agent's strategy when it participates in a given market only.
	 * @param m
	 * @param ts
	 * @return
	 */
	public abstract ActivityHashMap agentStrategy(Market m, TimeStamp ts);
	
	
	/**
	 * Agent arrives in a single market.
	 * 
	 * @param marketID
	 * @param arrivalTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(Market mkt, TimeStamp ts) {

		log.log(Log.INFO, ts.toString() + " | " + this.toString() + "->" + mkt.toString());
		this.enterMarket(mkt, ts);
		marketIDs.add(mkt.ID);
		
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateAllQuotes(this, ts));
		actMap.insertActivity(new AgentStrategy(this, mkt, ts));
		return actMap;
	}
	

	/**
	 * Agent departs a specified market, if it is active.
	 * 
	 * @param departureTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture(Market mkt) {

		mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.ID));
		mkt.buyers.remove(mkt.buyers.indexOf(this.ID));
		mkt.sellers.remove(mkt.sellers.indexOf(this.ID));
		mkt.removeBid(this.ID, null);
		this.exitMarket(mkt.ID);
		return null;
	}

}
