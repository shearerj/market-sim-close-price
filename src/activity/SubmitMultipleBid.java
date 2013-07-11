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
public class SubmitMultipleBid extends Activity {

	protected final Agent ag;
	protected final Market mkt;
	protected final Map<Price, Integer> priceQuantMap;

	public SubmitMultipleBid(Agent ag, Market mkt, Map<Price, Integer> priceQuantMap, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.mkt = mkt;
		this.priceQuantMap = priceQuantMap;
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return ag.executeSubmitMultipleBid(mkt, priceQuantMap, currentTime);
	}

	public String toString() {
		return new String(getName() + "::" + ag + "," + mkt + "+" + priceQuantMap);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubmitMultipleBid other = (SubmitMultipleBid) obj;
		return new EqualsBuilder().
				append(ag, other.ag).
				append(mkt, other.mkt).
				append(priceQuantMap, other.priceQuantMap).
				append(scheduledTime, other.scheduledTime).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(ag).
				append(mkt).
				append(priceQuantMap).
				append(scheduledTime).
				toHashCode();
	}
}
