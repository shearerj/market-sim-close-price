package entity;

import market.*;
import event.*;
import activity.*;
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
	
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		
		System.out.println("HFTAgentStrategy...");
		
		return null;
	}
	
}
