package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

import market.Quote;

/**
 * Template for ZIPAgent.
 * 
 * @author ewah
 *
 */
public class ZIPAgent extends SMAgent {

	private int margin;				// margin for limit order
	
	private double pvVar;			// variance from private value random process
	
	public ZIPAgent(int agentID, SystemData d, ObjectProperties p, Log l, int mktID) {
		super(agentID, d, p, l, mktID);
		agentType = Consts.getAgentType(this.getName());
		params = p;
		arrivalTime = new TimeStamp(0);
		pvVar = this.data.privateValueVar;
		privateValue = Math.max(0, this.data.nextPrivateValue() + 
				(int) Math.round(getNormalRV(0, pvVar)) * Consts.SCALING_FACTOR);
	}
	
	@Override
	public HashMap<String, Object> getObservation() {
		return null;
	}
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();

		
		/* TO FILL IN */
		
		
		// Insert events for the agent to sleep, then wake up again at timestamp tsNew
		int sleepTime = Integer.parseInt(params.get("sleepTime"));
		double sleepVar = Double.parseDouble(params.get("sleepVar"));
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, tsNew));
		return actMap;
	}
}
