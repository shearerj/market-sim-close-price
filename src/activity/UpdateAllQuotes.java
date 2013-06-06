package activity;


import java.util.Collection;

import entity.*;
import event.*;

/**
 * Class for Activity of getting quotes from all markets.
 * 
 * @author ewah
 */
public class UpdateAllQuotes extends Activity {

	private Agent ag;
	
	public UpdateAllQuotes(Agent ag, TimeStamp t) {
		this.ag = ag;
		this.time = t;
	}
	
	public UpdateAllQuotes deepCopy() {
		return new UpdateAllQuotes(this.ag, this.time);
	}
	
	public Collection<Activity> execute() {
		return this.ag.updateAllQuotes(this.time);
	}
	
	public String toString() {
		return new String("UpdateAllQuotes::" + this.ag.toString());
	}
}
