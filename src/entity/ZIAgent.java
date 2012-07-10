package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.Iterator;
import java.util.Random;

/**
 * Zero-intelligence agent.
 * 
 * @author ewah
 */
public class ZIAgent extends Agent {

	public double privateValue;
	
	private Random rand;
	
	/**
	 * Overloaded constructor.
	 * @param agentID
	 * @param d SystemData object
	 */
	public ZIAgent(int agentID, SystemData d) {
		super(agentID, d);
		agentType = "ZI";
		
		// TESTING
		rand = new Random();
		privateValue = 10*rand.nextDouble()+50;
		sleepTime = 20;
	}
	
	
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		
		System.out.println("ZIAgent " + this.ID + " ACTIVITY: ZIAgentStrategy");
		
		ActivityHashMap actMap = new ActivityHashMap();
		// Cycle through all markets & submit bids
		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());

			double p = privateValue + rand.nextDouble()*5;
			p = (double) Math.round(p * 10) / 10;
			int q = 1;
			if (rand.nextBoolean() == true) q = -1;
			
			actMap.appendActivityHashMap(addBid(mkt, p, q, ts));
		}
		if (!marketIDs.isEmpty()) {
			actMap.insertActivity(new UpdateAllQuotes(this, ts.sum(new TimeStamp(sleepTime))));
			actMap.insertActivity(new AgentStrategy(this, ts.sum(new TimeStamp(sleepTime))));
		}
		return actMap;
	}
}
