package entity;

import event.*;
import activity.*;
import systemmanager.*;

/**
 * Single market (SM) agent, whose agent strategy is executed only within its market.
 * 
 * @author ewah
 */
public abstract class SMAgent extends Agent {

	protected Market market;

	/**
	 * Constructor for a single market agent.
	 * @param agentID
	 * @param d
	 * @param p
	 * @param l
	 * @param mktID		sets the main market for the SMAgent
	 */
	public SMAgent(int agentID, SystemData d, ObjectProperties p, Log l, int mktID) {
		super(agentID, d, p, l);
		this.market = data.getMarket(mktID);
	}

	
	public Market getMainMarket() {
		return market;
	}
	
	/**
	 * @return main market ID for the single market agent.
	 */
	public int getMainMarketID() {
		return market.getID();
	}
	
	/**
	 * Agent arrives in a single market.
	 * 
	 * @param market
	 * @param ts
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(TimeStamp ts) {

		log.log(Log.INFO, ts.toString() + " | " + this.toString() + "->" + market.toString());
		this.enterMarket(market, ts);
		marketIDs.add(market.ID);
		
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateAllQuotes(this, ts));
		actMap.insertActivity(new AgentStrategy(this, market, ts));
		return actMap;
	}
	

	/**
	 * Agent departs a specified market, if it is active.
	 * 
	 * @param market
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture() {

		market.agentIDs.remove(market.agentIDs.indexOf(this.ID));
		market.buyers.remove(market.buyers.indexOf(this.ID));
		market.sellers.remove(market.sellers.indexOf(this.ID));
		market.removeBid(this.ID, null);
		this.exitMarket(market.ID);
		return null;
	}

}
