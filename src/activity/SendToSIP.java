package activity;

import java.util.Collection;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of sending new quote information to the Security Information Processor (SIP).
 * 
 * @author ewah
 */
public class SendToSIP extends Activity {

	private Market mkt;
	
	public SendToSIP(Market mkt, TimeStamp t) {
		this.mkt = mkt;
		this.time = t;
	}
	
	public SendToSIP deepCopy() {
		return new SendToSIP(this.mkt, this.time);
	}
	
	public Collection<Activity> execute() {
		return this.mkt.sendToSIP(this.time);
	}
	
	public String toString() {
		return new String("SendToSIP::" + this.mkt);
	}
}
