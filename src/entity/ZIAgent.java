package entity;

import event.*;
import activity.*;
import systemmanager.*;

/**
 * Zero-intelligence agent.
 * 
 * @author ewah
 */
public class ZIAgent extends SMAgent {

	public int privateValue;
	
	/**
	 * Overloaded constructor.
	 * @param agentID
	 * @param d SystemData object
	 */
	public ZIAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = "ZI";
		arrivalTime = new TimeStamp(0);
		sleepTime = Integer.parseInt(p.get(agentType).get("sleepTime"));
		sleepVar = Double.parseDouble(p.get(agentType).get("sleepVar"));
		privateValue = 1000*rand.nextInt(25)+10000;
	}
	
	
	public ActivityHashMap agentStrategy(Market mkt, TimeStamp ts) {
		
		if (mkt == null) return null;
		
		ActivityHashMap actMap = new ActivityHashMap();
		
		int p = privateValue + rand.nextInt(25)*1000;
		int q = 1;
		if (rand.nextBoolean() == true) q++;
		if (rand.nextDouble() < 0.5) q = -q;
		actMap.appendActivityHashMap(addBid(mkt, p, q, ts));
		
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime()));
		actMap.insertActivity(new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(new AgentStrategy(this, mkt, tsNew));
		return actMap;
	}
	
	
}
