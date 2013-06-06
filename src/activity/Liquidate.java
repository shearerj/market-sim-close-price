package activity;

import java.util.Collection;

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
		return new Liquidate(this.ag, this.p, this.time);
	}
	
	public Collection<Activity> execute() {
		return this.ag.executeLiquidate(this.p, this.time);
	}
	
	public String toString() {
		return new String("Liquidate::" + this.ag + " at " + this.p);
	}
}
