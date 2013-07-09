package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of withdrawing an agent's bid in a given market. Used when
 * bids expire as well.
 * 
 * @author ewah
 */
public class WithdrawBid extends Activity {

	private Agent ag;
	private Market mkt;

	public WithdrawBid(Agent ag, Market mkt, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.mkt = mkt;
	}

	public WithdrawBid deepCopy() {
		return new WithdrawBid(this.ag, this.mkt, this.scheduledTime);
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.ag.executeWithdrawBid(this.mkt, time);
	}

	public String toString() {
		return new String(getName() + "::" + ag + "," + mkt);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WithdrawBid other = (WithdrawBid) obj;
		return new EqualsBuilder().
				append(ag.getID(), other.ag.getID()).
				append(mkt.getID(), other.mkt.getID()).
				append(scheduledTime.longValue(), other.scheduledTime.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(ag.getID()).
				append(mkt.getID()).
				append(scheduledTime.longValue()).
				toHashCode();
	}
}
