package entity;

import activity.*;
import event.TimeStamp;
import systemmanager.*;

import java.util.Iterator;


/**
 * High-frequency trader (HFT) or multi-market agent. An HFTAgent arrives in 
 * all markets in a model, and its strategy is executed across multiple markets.
 * 
 * An HFTAgent is capable of seeing the quotes in multiple markets with zero delay.
 * These agents also bypass Regulation NMS restrictions as they have access to 
 * private data feeds, enabling them to compute their own version of the NBBO.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends Agent {

	/**
	 * Constructor for an HFT or multi-market agent.
	 * @param agentID
	 * @param modelID
	 * @param d
	 * @param p
	 * @param l
	 */
	public HFTAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
	}
	
	/**
	 * Agent arrives in all markets.
	 * 
	 * @param ts
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(TimeStamp ts) {
		
		String s = "";
		for (Iterator<Integer> it = this.getModel().getMarketIDs().iterator(); it.hasNext(); ) {
			Market mkt = data.markets.get(it.next());
			this.enterMarket(mkt, ts);
			s += mkt.toString();
			if (it.hasNext()) {
				s += ",";
			}
		}
		
		log.log(Log.INFO, ts.toString() + " | " + this.toString() + "->" + s);
			
		// Always insert agent strategy call once it has arrived in the market;
		// inserted at earliest priority in case there are orders already present at the beginning
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(Consts.HFT_ARRIVAL_PRIORITY, new UpdateAllQuotes(this, ts));
		actMap.insertActivity(Consts.HFT_ARRIVAL_PRIORITY, new AgentStrategy(this, ts));
		return actMap;
	}
	
	/**
	 * Agent re-enters/wakes up to execute its strategy.
	 * 
	 * @param priority
	 * @param ts
	 */
	public ActivityHashMap agentReentry(int priority, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(priority, new UpdateAllQuotes(this, ts));
		actMap.insertActivity(priority, new AgentStrategy(this, ts));
		return actMap;
	}
	
	/**
	 * Agent departs all markets, if it is active.  //TODO fix later
	 * 
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture(TimeStamp ts) {

		for (Iterator<Integer> i = data.getMarketIDs().iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			
			mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.ID));
			mkt.buyers.remove(mkt.buyers.indexOf(this.ID));
			mkt.sellers.remove(mkt.sellers.indexOf(this.ID));
			mkt.removeBid(this.ID, ts);
			this.exitMarket(mkt.ID);
		}
		ActivityHashMap actMap = new ActivityHashMap();
		return actMap;
	}
}
