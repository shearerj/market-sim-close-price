package activity;

import java.util.Collection;

import market.Price;

import org.apache.commons.lang3.builder.*;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of SIP processing a quote received from a given Market.
 * 
 * @author ewah
 */
public class ProcessQuote extends Activity {

	private IP ip;
	private Market market;
	private Price bid;
	private Price ask;

	public ProcessQuote(IP ip, Market market, Price bid, Price ask, TimeStamp t) {
		super(t);
		this.ip = ip;
		this.market = market;
		this.bid = bid;
		this.ask = ask;
	}

	public ProcessQuote deepCopy() {
		return new ProcessQuote(this.ip, this.market, this.bid, this.ask, this.scheduledTime);
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.ip.processQuote(this.market, this.bid, this.ask, time);
	}

	public String toString() {
		return new String(getName() + "::" + market + ":(" + bid + "," + ask + ")");
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
				append(market.getID(), other.market.getID()).
				append(ip.getID(), other.ip.getID()).
				append(bid, other.bid).
				append(ask, other.ask).
				append(scheduledTime.longValue(), other.scheduledTime.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(market.getID()).
				append(ip.getID()).
				append(bid).
				append(ask).
				append(scheduledTime.longValue()).
				toHashCode();
	}
}
