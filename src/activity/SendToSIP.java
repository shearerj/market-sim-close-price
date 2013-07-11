package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of sending new quote information to the Security Information Processor (SIP).
 * 
 * @author ewah
 */
public class SendToSIP extends Activity {

	private Market mkt;

	public SendToSIP(Market mkt, TimeStamp t) {
		super(t);
		this.mkt = mkt;
	}

	public SendToSIP deepCopy() {
		return new SendToSIP(this.mkt, this.scheduledTime);
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.mkt.sendToSIP(time);
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
		SendToSIP other = (SendToSIP) obj;
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
