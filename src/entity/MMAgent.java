package entity;

import data.*;
import activity.*;
import event.TimeStamp;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
/**
 * MMAGENT
 * 
 * Multi-market agent. An MMAgent arrives in all markets in a model, and its 
 * strategy is executed across multiple markets.
 * 
 * An MMAgent is capable of seeing the quotes in multiple markets with zero delay.
 * These agents also bypass Regulation NMS restrictions as they have access to 
 * private data feeds, enabling them to compute their own version of the NBBO.
 * 
 * @author ewah
 */
public abstract class MMAgent extends Agent {

	/**
	 * Constructor for a multi-market agent.
	 * 
	 * @param agentID
	 * @param modelID
	 * @param d
	 * @param p
	 * @param l
	 */
	public MMAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
	}
	
	/**
	 * Agent arrives in a single market.
	 * 
	 * @param ts
	 * @return Collection<Activity>
	 */
	public Collection<Activity> agentArrival(TimeStamp ts) {
		
		StringBuilder sb = new StringBuilder();
		for (Integer id : this.getModel().getMarketIDs()) {
			Market mkt = data.markets.get(id);
			this.enterMarket(mkt, ts);
			sb.append(mkt).append(",");
		}
		log.log(Log.INFO, ts.toString() + " | " + this + "->" + 
				sb.substring(0, sb.length() - 1));
		
		// Insert agent strategy call once it has arrived in the market
		Collection<Activity> actMap = new ArrayList<Activity>();
		actMap.add(new AgentStrategy(this, ts));
		return actMap;
	}
	
	/**
	 * Agent departs all markets, if it is active.  //TODO fix later
	 * 
	 * @return Collection<Activity>
	 */
	public Collection<Activity> agentDeparture(TimeStamp ts) {
		for (Integer id : data.getMarketIDs()) {
			Market mkt = data.markets.get(id);
			
			mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.id));
			mkt.buyers.remove(mkt.buyers.indexOf(this.id));
			mkt.sellers.remove(mkt.sellers.indexOf(this.id));
			mkt.removeBid(this.id, ts);
			this.exitMarket(id);
		}
		return Collections.emptyList();
	}
	
	/**
	 * Updates quotes for all markets.
	 * 
	 * @param ts
	 * @return
	 */
	public Collection<Activity> updateAllQuotes(TimeStamp ts) {
		for (Integer id : data.getModel(modelID).getMarketIDs()) {
			Market mkt = data.getMarket(id);
			updateQuotes(mkt, ts);
		}
		return this.executeUpdateAllQuotes(ts);
	}
}
