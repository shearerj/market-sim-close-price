package entity;

import event.*;
import activity.*;
import systemmanager.*;

/**
 * Single market (SM) agent, whose agent strategy is executed only within one market.
 * This does not mean that it can only trade with its specified market; however, it is
 * only capable of looking at price quotes from the NBBO and its market.
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
		market = data.getMarket(mktID);
	}

	/**
	 * @return market
	 */
	public Market getMarket() {
		return market;
	}
	
	/**
	 * @return main market ID for the single market agent.
	 */
	public int getMarketID() {
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

		log.log(Log.INFO, ts.toString() + " | " + this + "->" + market.toString());
		this.enterMarket(market, ts);
		
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
		ActivityHashMap actMap = new ActivityHashMap();
		return actMap;
	}

}
