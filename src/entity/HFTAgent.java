package entity;

import activity.Activity;
import activity.AgentStrategy;
import activity.UpdateAllQuotes;
import event.*;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Collection;
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
	 * Agent arrives in a single market.
	 * 
	 * @param ts
	 * @return Collection<Activity>
	 */
	public Collection<Activity> agentArrival(TimeStamp ts) {
		
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
		Collection<Activity> actMap = new ArrayList<Activity>();
		actMap.add(new UpdateAllQuotes(this, ts));
		actMap.add(new AgentStrategy(this, ts));
		return actMap;
	}
	
	/**
	 * Agent re-enters/wakes up to execute its strategy.
	 * 
	 * @param priority
	 * @param ts
	 */
	public Collection<Activity> agentReentry(int priority, TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		actMap.add(new UpdateAllQuotes(this, ts));
		actMap.add(new AgentStrategy(this, ts));
		return actMap;
	}
	
	/**
	 * Agent departs all markets, if it is active.  //TODO fix later
	 * 
	 * @return Collection<Activity>
	 */
	public Collection<Activity> agentDeparture(TimeStamp ts) {

		for (Iterator<Integer> i = data.getMarketIDs().iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			
			mkt.agentIDs.remove(mkt.agentIDs.indexOf(this.id));
			mkt.buyers.remove(mkt.buyers.indexOf(this.id));
			mkt.sellers.remove(mkt.sellers.indexOf(this.id));
			mkt.removeBid(this.id, ts);
			this.exitMarket(mkt.id);
		}
		Collection<Activity> actMap = new ArrayList<Activity>();
		return actMap;
	}
}
