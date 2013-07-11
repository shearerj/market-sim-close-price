package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
import market.*;
import event.TimeStamp;

/**
 * Class for Activity to liquidate an agent's position, based on some given price.
 * This price may be based on the value of the global fundamental.
 * 
 * @author ewah
 */
public class Liquidate extends Activity {

	private Agent ag;
	private Price p;

	public Liquidate(Agent ag, Price p, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.p = p;
	}

	public Liquidate deepCopy() {
		return new Liquidate(this.ag, this.p, this.scheduledTime);
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return this.ag.executeLiquidate(this.p, currentTime);
	}

	public String toString() {
		return new String(getName() + "::" + ag + " @" + p);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Liquidate other = (Liquidate) obj;
		return new EqualsBuilder().
				append(ag.getID(), other.ag.getID()).
				append(p.getPrice(), other.p.getPrice()).
				append(scheduledTime.longValue(), other.scheduledTime.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(ag.getID()).
				append(p.getPrice()).
				append(scheduledTime.longValue()).
				toHashCode();
	}
}
