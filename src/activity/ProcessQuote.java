package activity;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of SIP processing a quote received from a given Market.
 * 
 * @author ewah
 */
public class ProcessQuote extends Activity {

	private Quoter sip;
	private Market mkt;
	private int bid;
	private int ask;
	
	public ProcessQuote(Quoter sip, Market mkt, int bid, int ask, TimeStamp t) {
		this.sip = sip;
		this.mkt = mkt;
		this.bid = bid;
		this.ask = ask;
		this.time = t;
	}
	
	public ProcessQuote deepCopy() {
		return new ProcessQuote(this.sip, this.mkt, this.bid, this.ask, this.time);
	}
	
	public ActivityHashMap execute() {
		return this.sip.processQuote(this.mkt, this.bid, this.ask, this.time);
	}
	
	public String toString() {
		return new String("ProcessQuote::" + mkt + ":(" + this.bid + "," + this.ask + ")");
	}
}
