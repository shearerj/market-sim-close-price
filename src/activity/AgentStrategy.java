package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import event.*;

/**
 * Class for executing agent strategies.
 * 
 * @author ewah
 */
public class AgentStrategy extends Activity {

	private Agent ag;
	private Market mkt;

	public AgentStrategy(Agent ag, TimeStamp t) {
		this(ag, null, t);
	}

	public AgentStrategy(Agent ag, Market mkt, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.mkt = mkt;
	}

	public AgentStrategy deepCopy() {
		return new AgentStrategy(this.ag, this.mkt, this.time);
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return ag.agentStrategy(currentTime);
	}

	@Override
	public String toString() {
		if (mkt == null) {
			return new String(getName() + "::" + ag);
		} else {
			return new String(getName() + "::" + ag + "," + mkt);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AgentStrategy other = (AgentStrategy) obj;
		if (mkt == null) {
			return new EqualsBuilder().
					append(ag.getID(), other.ag.getID()).
					append(time.longValue(), other.time.longValue()).
					isEquals();
		} else {
			return new EqualsBuilder().
					append(ag.getID(), other.ag.getID()).
					append(mkt.getID(), other.mkt.getID()).
					append(time.longValue(), other.time.longValue()).
					isEquals();
		}
	}
	
	@Override
	public int hashCode() {
		if (mkt == null) {
			return new HashCodeBuilder(19, 37).
					append(ag.getID()).
					append(time.longValue()).
					toHashCode();
		} else {
			return new HashCodeBuilder(19, 37).
					append(ag.getID()).
					append(mkt.getID()).
					append(time.longValue()).
					toHashCode();
		}
	}
}
