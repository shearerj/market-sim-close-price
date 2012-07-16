package activity;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of getting the National Best Bid and Offer (NBBO).
 * 
 * @author ewah
 */
public class UpdateNBBO extends Activity {

	private Agent ag;
	
	public UpdateNBBO(Agent ag, TimeStamp t) {
		this.ag = ag;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.updateNBBO(this.time);
	}
	
}
