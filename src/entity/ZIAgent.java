package entity;

import event.*;
import activity.*;
import activity.market.*;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Zero-intelligence agent.
 * 
 * @author ewah
 */
public class ZIAgent extends Agent {

	public double privateValue;
	public double arrivalRate;
	
	/**
	 * Overloaded constructor.
	 * @param agentID
	 */
	public ZIAgent(int agentID, SystemData d) {
		super(agentID, d);
		agentType = "ZI";
	}
	
	public ActivityHashMap agentStrategy() {
		
		System.out.println("ACTIVITY: ZIAgentStrategy");
		
		// generate activity hash map
		ActivityHashMap actMap = new ActivityHashMap();
		int ts = 0;
		
		// now add activities to the activity map // TODO - try cycling through all market
		for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market m = data.markets.get(i.next());
			ts += 20;
			actMap.insertActivity(new SubmitBid(this, new PQBid(), m, new TimeStamp(ts)));
		}

		return actMap;
	}
}
