package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import model.*;
import event.TimeStamp;

/**
 * Class for Activity of computing the National Best Bid and Offer (NBBO) for a given MarketModel.
 * 
 * @author ewah
 */
public class UpdateNBBO extends Activity {

	private AbstractIP sip;
	private MarketModel mdl;

	public UpdateNBBO(AbstractIP sip, MarketModel mdl, TimeStamp t) {
		super(t);
		this.sip = sip;
		this.mdl = mdl;
	}

	public UpdateNBBO deepCopy() {
		return new UpdateNBBO(this.sip, this.mdl, this.scheduledTime);
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.sip.updateNBBO(this.mdl, time);
	}

	public String toString() {
		return new String(getName() + "::" + mdl.getFullName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateNBBO other = (UpdateNBBO) obj;
		return new EqualsBuilder().
				append(sip.getID(), other.sip.getID()).
				append(mdl.getID(), other.mdl.getID()).
				append(scheduledTime.longValue(), other.scheduledTime.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(sip.getID()).
				append(mdl.getID()).
				append(scheduledTime.longValue()).
				toHashCode();
	}
}
