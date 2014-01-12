package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Random;

import activity.Activity;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Computes either a linear weighted moving average or an exponential WMA.
 * Linear is selected with a weight factor of 0.
 * 
 * @author ewah
 *
 */
public class WMAMarketMaker extends MarketMaker {

	private static final long serialVersionUID = -8566264088391504213L;

	private int numElements;
	private double weightFactor;	// for computing weighted MA
	protected EvictingQueue<Price> bidQueue;
	protected EvictingQueue<Price> askQueue;

	protected WMAMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, double reentryRate, int tickSize, boolean noOp,
			int numRungs, int rungSize, boolean truncateLadder, int windowLength, 
			double weightFactor) {
		super(fundamental, sip, market, rand, reentryRate, tickSize, noOp, 
				numRungs, rungSize, truncateLadder);

		checkArgument(weightFactor >= 0, "Weight factor must be nonnegative!");
		checkArgument(windowLength > 0, "Window length must be positive!");
		
		this.weightFactor = weightFactor;
		numElements = windowLength; 
		bidQueue = EvictingQueue.create(windowLength);
		askQueue = EvictingQueue.create(windowLength);
	}

	public WMAMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, EntityProperties props) {
		this(fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.0005),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsBoolean(Keys.NO_OP, false),
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000), 
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true),
				props.getAsInt(Keys.WINDOW_LENGTH, 5),
				props.getAsDouble(Keys.WEIGHT_FACTOR, 0));
	}

	@Override
	public Iterable<Activity> agentStrategy(TimeStamp currentTime) {
		if (noOp) return ImmutableList.of(); // no execution if no-op

		Builder<Activity> acts = ImmutableList.<Activity> builder().addAll(
				super.agentStrategy(currentTime));

		lastNBBOQuote = sip.getNBBO();
		Quote quote = marketQuoteProcessor.getQuote();
		Price bid = quote.getBidPrice();
		Price ask = quote.getAskPrice();

		// Quote changed, withdraw all orders
		if ((bid == null && lastBid != null)
				|| (bid != null && !bid.equals(lastBid))
				|| (bid != null && lastBid == null)
				|| (ask == null && lastAsk != null)
				|| (ask != null && !ask.equals(lastAsk))
				|| (ask != null && lastAsk == null)) {
			acts.addAll(withdrawAllOrders(currentTime));	

			if (!quote.isDefined()) {
				log(INFO, this + " " + getName()
						+ "::agentStrategy: undefined quote in market "
						+ primaryMarket);
			} else {
				lastNBBOQuote = sip.getNBBO();
				bid = marketQuoteProcessor.getQuote().getBidPrice();
				ask = marketQuoteProcessor.getQuote().getAskPrice();

				if (bidQueue.isEmpty()) {
					// if queues are empty, fill in all with the same bid/ask
					for (int i = 0; i < numElements; i++) {
						bidQueue.add(bid);
						askQueue.add(ask);
					}
				} else {
					bidQueue.add(bid);
					askQueue.add(ask);
				}

				// Compute weighted moving average
				double sumBids = 0, sumAsks = 0;
				double totalWeight = 0;
				if (weightFactor == 0) {
					// Linearly weighted moving average
					int i = 0;
					for (Price x : bidQueue) {
						sumBids += (++i) * x.intValue();
						totalWeight += i;
					}
					i = 0;
					for (Price y : askQueue) {
						sumAsks += (++i) * y.intValue();
					}
				} else {
					// Exponential WMA
					int i = 0;
					for (Price x : bidQueue) {
						double weight = weightFactor * Math.pow(1-weightFactor, i++);
						sumBids += weight * x.intValue();
						totalWeight += weight;
					}
					i = 0;
					for (Price y : askQueue) {
						double weight = weightFactor * Math.pow(1-weightFactor, i++);
						sumAsks += weight * y.intValue();
					}
				}
				Price ladderBid = new Price(sumBids / totalWeight);
				Price ladderAsk = new Price(sumAsks / totalWeight);

				acts.addAll(this.createOrderLadder(ladderBid, ladderAsk, currentTime));

			} // if quote defined
		} else {
			log(INFO, currentTime + " | " + primaryMarket + " " + this + " " + getName()
					+ "::agentStrategy: no change in submitted ladder.");
		}
		// update latest bid/ask prices
		lastAsk = ask;
		lastBid = bid;

		return acts.build();
	}

	@Override
	public String toString() {
		return "WMAMarketMaker " + super.toString();
	}
}