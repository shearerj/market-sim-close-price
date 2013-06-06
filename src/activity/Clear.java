package activity;

import java.util.Collection;

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
		super(t);
		this.mkt = mkt;
	}
	
	public Clear deepCopy() {
		return new Clear(this.mkt, this.time);
	}
	
	public Collection<Activity> execute() {
		return this.mkt.clear(this.time);
	}
	
	public String toString() {
		return new String("Clear::" + this.mkt.toString());
	}
}
