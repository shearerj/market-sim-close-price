package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.Iterator;
import java.util.Random;
import java.util.Properties;

/**
 * Zero-intelligence agent.
 * 
 * @author ewah
 */
public class ZIAgent extends Agent {

	public int privateValue;
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
		privateValue = 1000*rand.nextInt(25)+10000;
	}
	
	
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		
		System.out.println(agentType + "Agent " + this.ID + ": AgentStrategy");
		
		ActivityHashMap actMap = new ActivityHashMap();
		// Cycle through all markets & submit bids
		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());

			int p = privateValue + rand.nextInt(25)*1000;
			int q = 1;
			if (rand.nextBoolean() == true) q++;
			if (rand.nextDouble() < 0.5) q = -q;
			
			actMap.appendActivityHashMap(addBid(mkt, p, q, ts));
		}
		if (!marketIDs.isEmpty()) {
			TimeStamp tsNew = ts.sum(new TimeStamp(sleepTime));
			actMap.insertActivity(new UpdateAllQuotes(this, tsNew));
			actMap.insertActivity(new AgentStrategy(this, tsNew));
		}
		return actMap;
	}
}
