package activity;

import entity.*;
import event.*;

/**
 * Class for activity of submitting a Bid to a market.
 * 
 * @author ewah
 */
public class SubmitBid extends BidActivity {
	
	private Bid bid;
	private Agent ag;
	
	
	public SubmitBid(Entity en, Bid b) {
		this.ag = (Agent) en;
		this.bid = b;
	}
	
	/* (non-Javadoc)
	 * @see activity.Activity#execute()
	 */
	public Event execute() {
		return this.ag.submitBid(this.bid); // need to add the market ID
	}
}