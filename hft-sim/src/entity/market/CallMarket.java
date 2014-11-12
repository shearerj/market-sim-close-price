package entity.market;

import static com.google.common.base.Preconditions.checkArgument;
import logger.Log;
import systemmanager.Keys.ClearInterval;
import systemmanager.Keys.MarketTickSize;
import systemmanager.Keys.PricingPolicy;
import systemmanager.Keys.TickSize;
import utils.Rand;
import data.Props;
import data.Stats;
import entity.market.clearingrule.UniformPriceClear;
import entity.sip.MarketInfo;
import event.Activity;
import event.TimeStamp;
import event.Timeline;

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

	protected CallMarket(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, Props props) {
		super(id, stats, timeline, log, rand, sip,
				new UniformPriceClear(props.get(PricingPolicy.class), props.get(MarketTickSize.class, TickSize.class)),
				props);
		this.clearFreq = props.get(ClearInterval.class);
		checkArgument(clearFreq.after(TimeStamp.ZERO), "Can't clear at frequency zero");
		scheduleActivityIn(TimeStamp.ZERO, new Activity() {
			@Override public void execute() { CallMarket.this.clear(); }
			@Override public String toString() { return "First Clear"; }
		});
	}

	public static CallMarket create(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, Props props) {
		return new CallMarket(id, stats, timeline, log, rand, sip, props);
	}

	@Override
	protected void clear() {
		super.clear();
		scheduleActivityIn(clearFreq, new Activity() {
			@Override public void execute() { CallMarket.this.clear(); }
			@Override public String toString() { return "Clear"; }
		});
	}

}
