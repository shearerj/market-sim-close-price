package entity.market;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Simulation;
import data.Props;
import event.Activity;
import event.TimeStamp;

/**
 * Class for a call market. The order book is closed, therefore agents will only
 * be able to see the price of the last clear as well as the bid/ask immediately
 * after the clear, i.e. they will be able to see the best available buy and
 * sell prices for the bids left in the order book after each market clear.
 * 
 * NOTE: First Clear Activity is initialized in the SystemManager.
 * 
 * @author ewah
 */
public class CallMarket extends Market {

	private static final long serialVersionUID = -1736458709580878467L;

	private final TimeStamp clearFreq;

	protected CallMarket(Simulation sim, Random rand, Props props) {
		super(sim, new UniformPriceClear(props.getAsDouble(Keys.PRICING_POLICY), props.getAsInt(Keys.MARKET_TICK_SIZE, Keys.TICK_SIZE)),
				rand, props);
		this.clearFreq = TimeStamp.of(props.getAsInt(Keys.CLEAR_FREQ));
		checkArgument(clearFreq.after(TimeStamp.ZERO), "Can't clear at frequency zero");
		sim.scheduleActivityIn(TimeStamp.IMMEDIATE, new Activity() {
			@Override public void execute() { CallMarket.this.clear(); }
			@Override public String toString() { return "First Clear"; }
		});
	}

	public static CallMarket create(Simulation sim, Random rand, Props props) {
		return new CallMarket(sim, rand, props);
	}

	@Override
	protected void clear() {
		super.clear();
		sim.scheduleActivityIn(clearFreq, new Activity() {
			@Override public void execute() { CallMarket.this.clear(); }
			@Override public String toString() { return "Clear"; }
		});
	}

}
