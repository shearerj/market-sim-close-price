package entity;

import event.*;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {

	public Agent() {
		// empty constructor
	}
	
	public Agent(int agentID) {
		this.entityID = agentID;
	}
	
	// Abstract function specifying agent's strategy for participating in the market.
	public abstract Event agentStrategy();
	
	// Abstract function specifying when agent arrives
	public abstract Event agentArrival();  		// marketID? but can arrive in multiple markets 
	
	
	// functions to submit bids and other interactions with the market
	public Event submitBid(Bid b) { //), Integer marketID) {
		// TODO
		
		System.out.println("Bid " + b.bidID + " has been submitted.");
		
		// dummy contents for now
		return null;
	}
	
}
