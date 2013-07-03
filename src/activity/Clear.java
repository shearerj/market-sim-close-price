package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
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
		return new Clear(this.mkt, this.time);
	}

	public Collection<Activity> execute(TimeStamp currentTime) {
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
				append(time.longValue(), other.time.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(mkt.getID()).
				append(time.longValue()).
				toHashCode();
	}
}
