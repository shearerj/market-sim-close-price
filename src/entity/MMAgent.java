package entity;

import java.util.Iterator;
import java.util.ArrayList;

import activity.*;
import event.TimeStamp;
import systemmanager.*;


/**
 * Multi-market (MM) agent, whose agent strategy is called once for all markets.
 * 
 * @author ewah
 */
public abstract class MMAgent extends Agent {

	/**
	 * Constructore for a multimarket agent.
	 * @param agentID
	 * @param d
	 * @param p
	 * @param l
	 */
	public MMAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		
		marketIDs = new ArrayList<Integer>(d.markets.keySet());
	}
	
	/**
	 * Function specifying agent's strategy when it participates in all markets.
	 * 
	 * @param ts
	 * @return
	 */
	public abstract ActivityHashMap agentStrategy(TimeStamp ts);
	
	/**
	 * Agent arrives in a single market.
	 * 
	 * @param marketID
	 * @param arrivalTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(TimeStamp ts) {
		
		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			this.enterMarket(mkt, ts);
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + "->" + mkt.toString());
		}
		if (sleepTime > 0) {
			ActivityHashMap actMap = new ActivityHashMap();
			actMap.insertActivity(new UpdateAllQuotes(this, ts));
			actMap.insertActivity(new AgentStrategy(this, ts));
			return actMap;
		} else {
			return null;
		}
	}
	
	/**
	 * Agent departs a specified market, if it is active.
	 * 
	 * @param departureTime
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture() {

		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			
			mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.ID));
			mkt.buyers.remove(mkt.buyers.indexOf(this.ID));
			mkt.sellers.remove(mkt.sellers.indexOf(this.ID));
			mkt.removeBid(this.ID);
			this.exitMarket(mkt.ID);
		}
		return null;
	}
}
