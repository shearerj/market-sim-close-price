package entity;

import java.util.Iterator;
import java.util.ArrayList;

import activity.*;
import event.TimeStamp;
import systemmanager.*;


/**
 * Multi-market (MM) agent. It arrives in all markets in a model, and its strategy is
 * executed across multiple markets.
 * 
 * @author ewah
 */
public abstract class MMAgent extends Agent {

	/**
	 * Constructor for a multi-market agent.
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
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(TimeStamp ts) {
		
		String s = "";
		for (Iterator<Integer> it = data.getModel(modelID).getMarketIDs().iterator(); it.hasNext(); ) {
			Market mkt = data.markets.get(it.next());
			this.enterMarket(mkt, ts);
			s += mkt.toString();
			if (it.hasNext()) {
				s += ",";
			}
		}
		
		log.log(Log.INFO, ts.toString() + " | " + this.toString() + "->" + s);
			
		// Always insert agent strategy call once it has arrived in the market
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateAllQuotes(this, ts));
		actMap.insertActivity(new AgentStrategy(this, ts));
		return actMap;
	}
	
	/**
	 * Agent departs all markets, if it is active.
	 * 
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture() {

		for (Iterator<Integer> i = data.getMarketIDs().iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			
			mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.ID));
			mkt.buyers.remove(mkt.buyers.indexOf(this.ID));
			mkt.sellers.remove(mkt.sellers.indexOf(this.ID));
			mkt.removeBid(this.ID, null);
			this.exitMarket(mkt.ID);
		}
		ActivityHashMap actMap = new ActivityHashMap();
		return actMap;
	}
}
