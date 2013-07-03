package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import event.*;

/**
 * Class for activity of agents arriving in a market or market(s).
 * 
 * @author ewah
 */
public class AgentArrival extends Activity {

	private Agent ag;
	
	public AgentArrival(Agent ag, TimeStamp t) {
		super(t);
		this.ag = ag;
	}
	
	public AgentArrival deepCopy() {
		return new AgentArrival(this.ag, this.time);
	}
	
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return ag.agentArrival(currentTime); 
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
		AgentArrival other = (AgentArrival) obj;
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
