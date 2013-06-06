package activity;

import java.util.Collection;

import entity.*;
import model.*;
import event.TimeStamp;

/**
 * Class for Activity of computing the National Best Bid and Offer (NBBO) for a given MarketModel.
 * 
 * @author ewah
 */
public class UpdateNBBO extends Activity {

	private SIP sip;
	private MarketModel mdl;
	
	public UpdateNBBO(SIP sip, MarketModel mdl, TimeStamp t) {
		this.sip = sip;
		this.mdl = mdl;
		this.time = t;
	}
	
	public UpdateNBBO deepCopy() {
		return new UpdateNBBO(this.sip, this.mdl, this.time);
	}
	
	public Collection<Activity> execute() {
		return this.sip.updateNBBO(this.mdl, this.time);
	}
	
	public String toString() {
		return new String("UpdateNBBO::" + mdl.getFullName());
	}
}
