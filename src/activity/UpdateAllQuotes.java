package activity;

import org.apache.commons.lang3.builder.*;

import java.util.Collection;

import entity.*;
import event.*;

/**
 * Class for Activity of getting quotes from all markets.
 * 
 * @author ewah
 */
public class UpdateAllQuotes extends Activity {

	private Agent ag;

	public UpdateAllQuotes(Agent ag, TimeStamp t) {
		super(t);
		this.ag = ag;
	}

	public UpdateAllQuotes deepCopy() {
		return new UpdateAllQuotes(this.ag, this.scheduledTime);
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.ag.executeUpdateAllQuotes(time);
	}

	public String toString() {
		return new String(getName() + "::" + ag);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateAllQuotes other = (UpdateAllQuotes) obj;
		return new EqualsBuilder().
				append(ag.getID(), other.ag.getID()).
				append(scheduledTime.longValue(), other.scheduledTime.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(ag.getID()).
				append(scheduledTime.longValue()).
				toHashCode();
	}
}
