package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.agent.*;
import entity.market.*;
import event.TimeStamp;

/**
 * Class for Activity to liquidate an agent's position, based on some given price.
 * This price may be based on the value of the global fundamental.
 * 
 * @author ewah
 */
// TODO Currently unused
public class Liquidate extends Activity {

	protected final Agent agent;
	protected final Price price;

	public Liquidate(Agent agent, Price price, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.agent = agent;
		this.price = price;
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return this.agent.liquidate(this.price, currentTime);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(price).append(
				super.hashCode()).toHashCode();
	}
	
	@Override
	public String toString() {
		return getName() + " :: " + agent + " @" + price;
	}
	
}
