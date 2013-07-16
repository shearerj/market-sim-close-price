package activity;

import entity.*;
import event.*;

import java.util.Collection;
import java.util.Map;

import market.Price;

import org.apache.commons.lang3.builder.*;

/**
 * Class for Activity of submitting a multiple-offer bid to a given market.
 * 
 * @author ewah
 */
public class SubmitMultiPointBid extends Activity {

	protected final Agent agent;
	protected final Market market;
	protected final Map<Price, Integer> priceQuantMap;
	protected final TimeStamp duration;

	public SubmitMultiPointBid(Agent agent, Market market,
			Map<Price, Integer> priceQuantMap, TimeStamp scheduledTime,
			TimeStamp duration) {
		super(scheduledTime);
		this.agent = agent;
		this.market = market;
		this.priceQuantMap = priceQuantMap;
		this.duration = duration;
	}
	
	public SubmitMultiPointBid(Agent agent, Market market,
			Map<Price, Integer> priceQuantMap, TimeStamp scheduledTime) {
		this(agent, market, priceQuantMap, scheduledTime, TimeStamp.IMMEDIATE);
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return market.submitMultiPointBid(agent, priceQuantMap, currentTime,
				duration);
	}

	public String toString() {
		return new String(getName() + "::" + agent + "," + market + "+"
				+ priceQuantMap);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SubmitMultiPointBid other = (SubmitMultiPointBid) obj;
		return new EqualsBuilder().append(agent, other.agent).append(market,
				other.market).append(priceQuantMap, other.priceQuantMap).append(
				scheduledTime, other.scheduledTime).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(market).append(
				priceQuantMap).append(scheduledTime).toHashCode();
	}
}
