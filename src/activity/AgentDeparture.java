package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import event.*;

/**
 * Class for activity of agents leaving a market or market(s).
 * 
 * @author ewah
 */
public class AgentDeparture extends Activity {

	private Agent ag;

	public AgentDeparture(Agent ag, TimeStamp t) {
		super(t);
		this.ag = ag;
	}

	public AgentDeparture deepCopy() {
		return new AgentDeparture(this.ag, this.time);
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return ag.agentDeparture(currentTime);
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
		AgentDeparture other = (AgentDeparture) obj;
		return new EqualsBuilder().
				append(ag.getID(), other.ag.getID()).
				append(time.longValue(), other.time.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(ag.getID()).
				append(time.longValue()).
				toHashCode();
	}
}
