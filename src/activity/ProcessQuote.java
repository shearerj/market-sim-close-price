package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of SIP processing a quote received from a given Market.
 * 
 * @author ewah
 */
public class ProcessQuote extends Activity {

	private SIP sip;
	private Market mkt;
	private int bid;
	private int ask;

	public ProcessQuote(SIP sip, Market mkt, int bid, int ask, TimeStamp t) {
		super(t);
		this.sip = sip;
		this.mkt = mkt;
		this.bid = bid;
		this.ask = ask;
	}

	public ProcessQuote deepCopy() {
		return new ProcessQuote(this.sip, this.mkt, this.bid, this.ask, this.time);
	}

	public Collection<Activity> execute(TimeStamp time) {
		return this.sip.processQuote(this.mkt, this.bid, this.ask, time);
	}

	public String toString() {
		return new String(getName() + "::" + mkt + ":(" + bid + "," + ask + ")");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProcessQuote other = (ProcessQuote) obj;
		return new EqualsBuilder().
				append(mkt.getID(), other.mkt.getID()).
				append(sip.getID(), other.sip.getID()).
				append(bid, other.bid).
				append(ask, other.ask).
				append(time.longValue(), other.time.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(mkt.getID()).
				append(sip.getID()).
				append(bid).
				append(ask).
				append(time.longValue()).
				toHashCode();
	}
}
