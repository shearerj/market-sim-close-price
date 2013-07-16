package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of withdrawing an agent's bid in a given market. Used when bids expire as
 * well.
 * 
 * @author ewah
 */
public class WithdrawBid extends Activity {

	private Agent agent;
	private Market market;

	public WithdrawBid(Agent agent, Market market, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.agent = agent;
		this.market = market;
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return market.withdrawBid(agent, time);
	}

	public String toString() {
		return new String(getName() + "::" + agent + "," + market);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof WithdrawBid)) return false;
		WithdrawBid other = (WithdrawBid) obj;
		return super.equals(obj) && this.agent.equals(other.agent)
				&& this.market.equals(other.market);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(market).append(
				scheduledTime).toHashCode();
	}
}
