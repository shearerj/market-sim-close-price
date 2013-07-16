package activity;

import java.util.Collection;

import market.PQBid;
import market.Price;
import market.Quote;

import org.apache.commons.lang3.builder.*;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of sending new quote information to the Security Information Processor (SIP).
 * 
 * @author ewah
 */
public class SendToIP extends Activity {

	private Market mkt;
	protected IP ip;
	protected Quote quote;

	public SendToIP(Market mkt, TimeStamp t, Quote quote, IP ip) {
		super(t);
		this.mkt = mkt;
		this.ip = ip;
		this.quote = quote;
	}
	
	public SendToIP(Market market, TimeStamp t) {
		super(t);
		this.mkt = market;
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.ip.sendToIP(mkt, quote.getBidPrice(), quote.getAskPrice(), time);
	}

	public String toString() {
		return new String(getName() + "::" + mkt);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SendToIP other = (SendToIP) obj;
		return new EqualsBuilder().
				append(mkt.getID(), other.mkt.getID()).
				append(scheduledTime.longValue(), other.scheduledTime.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(mkt.getID()).
				append(scheduledTime.longValue()).
				toHashCode();
	}
}
