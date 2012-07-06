package activity;


import entity.*;
import event.*;

/**
 * Class for Activity of getting a market's quote.
 * 
 * @author ewah
 */
public class UpdateQuotes extends Activity {

	private Agent ag;
	private Market mkt;
	
	public UpdateQuotes(Agent ag, Market m, TimeStamp t) {
		this.ag = ag;
		this.mkt = m;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.updateQuotes(this.mkt);
	}
}
