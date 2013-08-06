package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import entity.market.Market;
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
		return new Clear(this.mkt, this.scheduledTime);
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return this.mkt.clear(currentTime);
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
		Clear other = (Clear) obj;
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
