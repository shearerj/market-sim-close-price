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
	 * Constructor for a multimarket agent.
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
		
		String s = "";
		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			this.enterMarket(mkt, ts);
			s += mkt.toString() + ",";
		}
		
		s = s.substring(0, s.length() - 1);
		log.log(Log.INFO, ts.toString() + " | " + this.toString() + "->" + s);
		
//		boolean flag = false;
//		if (params.containsKey("sleepTime")) {
//			if (Integer.parseInt(params.get("sleepTime")) > 0) {
//				flag = true;
//			}
//		}
//		// Only insert agent strategy call if sleepTime is nonzero or if it's an NBBO agent
//		if (flag || this instanceof BackgroundAgent) {
//			ActivityHashMap actMap = new ActivityHashMap();
//			actMap.insertActivity(new UpdateAllQuotes(this, ts));
//			actMap.insertActivity(new AgentStrategy(this, ts));
//			return actMap;
//		} else {
//			return null;
//		}
		
		// Always insert agent strategy call once it's arrived in the market
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateAllQuotes(this, ts));
		actMap.insertActivity(new AgentStrategy(this, ts));
		return actMap;
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
			mkt.removeBid(this.ID, null);
			this.exitMarket(mkt.ID);
		}
		return null;
	}
}
