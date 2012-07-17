package activity;


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
	
	public ActivityHashMap execute() {
		return this.ag.updateAllQuotes(this.time);
	}
	
	public String toString() {
		return new String("UpdateAllQuotes(Agt " + this.ag.getID() + ")");
	}
}
