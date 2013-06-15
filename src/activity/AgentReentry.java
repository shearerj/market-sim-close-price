package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import event.*;
import entity.*;

/**
 * Class for activity of agents that re-enter a market or market(s).
 * 
 * @author ewah
 */
public class AgentReentry extends Activity {

	private Agent ag;

	public AgentReentry(Agent ag, TimeStamp t) {
		super(t);
		this.ag = ag;
	}

	public AgentReentry deepCopy() {
		return new AgentReentry(this.ag, this.time);
	}

	public Collection<Activity> execute(TimeStamp currentTime) {
		return ag.agentReentry(currentTime); 
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
		AgentReentry other = (AgentReentry) obj;
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
