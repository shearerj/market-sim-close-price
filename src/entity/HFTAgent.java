package entity;

import event.*;
import activity.*;
import activity.market.*;
import systemmanager.*;

/**
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * @author ewah
 */
public class HFTAgent extends Agent {
	
	/**
	 * Overloaded constructor
	 * @param agentID
	 */
	public HFTAgent(int agentID, SystemData d) {
		super(agentID, d);
		agentType = "HFT";
	}
	
	public ActivityHashMap agentStrategy() {
		
		System.out.println("HFTAgentStrategy...");
		
		return null;
	}
	
}
