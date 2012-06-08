package entity;

import java.util.LinkedList;
import java.util.Random;

import event.Event;
import event.TimeStamp;
import activity.*;

/**
 * Zero-intelligence agent.
 * 
 * @author ewah
 */
public class ZIAgent extends Agent {

	Random rand;
	
	/**
	 * Overloaded constructor.
	 * @param agentID
	 */
	public ZIAgent(int agentID) {
		this.entityID = agentID;
	}
	
	/* (non-Javadoc)
	 * @see entity.Agent#agentStrategy()
	 */
	public Event agentStrategy() {
		
		// dummy strategy is to submit a bid at a randomly generated time
		rand = new Random();
		int randBid = rand.nextInt(100);
		System.out.println("random bid generated is: " + randBid);
		
		// now generate a new event to be added
		Bid b = new Bid(10);
		Activity subBid1 = new SubmitBid(this, b);
		Activity subBid2 = new SubmitBid(this, b);
		
		// create event
		TimeStamp t = new TimeStamp(40);
		LinkedList<Activity> actToAdd = new LinkedList<Activity>();
		actToAdd.add(subBid1);
		Event e_test = new Event(t,actToAdd);
		e_test.storeActivity(subBid2);
		
		// return event to be added
		return e_test;
	}
	
	public Event agentArrival() {
		// TODO
		return null;
	}
	
}
