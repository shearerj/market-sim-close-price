package activity;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of clearing the orderbook.
 * 
 * @author ewah
 */
public class Clear extends Activity {
	
	private Market mkt;
	
	public Clear(Market mkt, TimeStamp t) {
		this.mkt = mkt;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.mkt.clear(this.time);
	}
	
	public String toString() {
		return new String("Clear(Mkt " + this.mkt.getID() + ")");
	}
}
