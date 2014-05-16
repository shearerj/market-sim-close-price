package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;

import com.google.common.collect.EvictingQueue;

import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * WMAMARKETMAKER
 * 
 * Weighted Moving Average Market Maker
 * 
 * Computes either a linear weighted moving average or an exponential WMA.
 * Linear is selected with a weight factor of 0.
 * 
 * The computation of the exponential weighted moving average is parameterized
 * by a weight factor w in range (0,1). The weight of a general data point with 
 * lag i is, before normalization:
 * 
 * 		w * ( 1-w )^i
 * 
 * A weight factor of 0 reverts the weighting to a linear weighted moving
 * average, which is computed with weight for a data point with lag i:
 * 
 * 		T-i
 * 
 * where T is the total number of elements being averaged.
 * 
 * NOTE: Because the prices are stored in an EvictingQueue, which does not
 * accept null elements, the number of elements in the bid/ask queues may not
 * be equivalent.
 * 
 * @author zzy, ewah
 *
 */
public class WMAMarketMaker extends MarketMaker {

	private static final long serialVersionUID = -8566264088391504213L;

	private double weightFactor;	// for computing weighted MA
	protected EvictingQueue<Price> bidQueue;
	protected EvictingQueue<Price> askQueue;

	protected WMAMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			int tickSize, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickInside,
			int initLadderMean, int initLadderRange, int numHistorical, 
			double weightFactor) {
		
		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				numRungs, rungSize, truncateLadder, tickImprovement, tickInside,
				initLadderMean, initLadderRange);

		checkArgument(weightFactor >= 0 && weightFactor < 1, 
				"Weight factor must be in range (0,1)!");
		checkArgument(numHistorical > 0, "Number of historical prices must be positive!");
		
		this.weightFactor = weightFactor;
		bidQueue = EvictingQueue.create(numHistorical);
		askQueue = EvictingQueue.create(numHistorical);
	}

	public WMAMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, EntityProperties props) {
		this(scheduler, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.0005),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000),
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true), 
				props.getAsBoolean(Keys.TICK_IMPROVEMENT, true),
				props.getAsBoolean(Keys.TICK_INSIDE, true),
				props.getAsInt(Keys.INITIAL_LADDER_MEAN, 0),
				props.getAsInt(Keys.INITIAL_LADDER_RANGE, 0),
				props.getAsInt(Keys.NUM_HISTORICAL, 5), 
				props.getAsDouble(Keys.WEIGHT_FACTOR, 0));
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		super.agentStrategy(currentTime);

		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();

		if (bid == null && lastBid == null && ask == null && lastAsk == null) {
			this.createOrderLadder(bid, ask);
			
		} else if ((bid == null && lastBid != null)
				|| (bid != null && !bid.equals(lastBid))
				|| (bid != null && lastBid == null)
				|| (ask == null && lastAsk != null)
				|| (ask != null && !ask.equals(lastAsk))
				|| (ask != null && lastAsk == null)) {	

			if (!this.getQuote().isDefined()) {
				log.log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
				this.createOrderLadder(bid, ask);
				
			} else {
				// Quote changed, still valid, withdraw all orders
				log.log(INFO, "%s in %s: Withdraw all orders", this, primaryMarket);
				withdrawAllOrders();
				
				bid = this.getQuote().getBidPrice();
				ask = this.getQuote().getAskPrice();

				// Use last known bid/ask if undefined post-withdrawal
				if (!this.getQuote().isDefined()) {
					Price oldBid = bid, oldAsk = ask;
					if (bid == null && lastBid != null) bid = lastBid;
					if (ask == null && lastAsk != null) ask = lastAsk;
					log.log(INFO, "%s in %s: Ladder MID (%s, %s)-->(%s, %s)", 
							this, primaryMarket, oldBid, oldAsk, bid, ask);
				}
				
				// Compute weighted moving average
				if (bid != null) bidQueue.add(bid);
				if (ask != null) askQueue.add(ask);
				
				Price ladderBid = null, ladderAsk = null;
				double sumBids = 0, sumAsks = 0;
				double bidTotalWeight = 0, askTotalWeight = 0;
				if (weightFactor == 0) {
					// Linearly weighted moving average
					int i = 0;
					for (Price x : bidQueue) {
						sumBids += (++i) * x.intValue();
						bidTotalWeight += i;
					}
					i = 0;
					for (Price y : askQueue) {
						sumAsks += (++i) * y.intValue();
						askTotalWeight += i;
					}
				} else {
					// Exponential WMA
					int i = bidQueue.size()-1;
					for (Price x : bidQueue) {
						double weight = weightFactor * Math.pow(1-weightFactor, i--);
						sumBids += weight * x.intValue();
						bidTotalWeight += weight;
					}
					i = askQueue.size()-1;
					for (Price y : askQueue) {
						double weight = weightFactor * Math.pow(1-weightFactor, i--);
						sumAsks += weight * y.intValue();
						askTotalWeight += weight;
					}
				}
				if (!bidQueue.isEmpty()) ladderBid = new Price(sumBids / bidTotalWeight);
				if (!askQueue.isEmpty()) ladderAsk = new Price(sumAsks / askTotalWeight);

				this.createOrderLadder(ladderBid, ladderAsk);
			} // if quote defined
			
		} else {
			log.log(INFO, "%s in %s: No change in submitted ladder", this, primaryMarket);
		}
		// update latest bid/ask prices
		lastAsk = ask;
		lastBid = bid;
	}
}