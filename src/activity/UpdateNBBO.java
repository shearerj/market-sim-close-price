package activity;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of getting the National Best Bid and Offer (NBBO).
 * 
 * @author ewah
 */
public class UpdateNBBO extends Activity {

	private Quoter q;
	
	public UpdateNBBO(Quoter q, TimeStamp t) {
		this.q = q;
		this.time = t;
	}
	
	public UpdateNBBO deepCopy() {
		return new UpdateNBBO(this.q, this.time);
	}
	
	public ActivityHashMap execute() {
		return this.q.updateNBBO(this.time);
	}
	
	public String toString() {
		return new String("UpdateNBBO()");
	}
}
