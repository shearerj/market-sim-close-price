package activity;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of getting the National Best Bid and Offer (NBBO).
 * 
 * @author ewah
 */
public class GetNBBO extends Activity {

	private Agent ag;
	
	public GetNBBO(Agent ag, TimeStamp t) {
		this.ag = ag;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.getNBBO(this.time);
	}
	
}
