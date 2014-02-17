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
			int tickSize, boolean noOp, int numRungs, int rungSize,
			boolean truncateLadder, boolean tickImprovement,
			boolean tickInside, int numHistorical, double weightFactor) {
		
		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				noOp, numRungs, rungSize, truncateLadder, tickImprovement,
				tickInside);

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
				props.getAsBoolean(Keys.NO_OP, false),
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000), 
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true),
				props.getAsBoolean(Keys.TICK_IMPROVEMENT, true),
				props.getAsBoolean(Keys.TICK_INSIDE, true),
				props.getAsInt(Keys.NUM_HISTORICAL, 5),
				props.getAsDouble(Keys.WEIGHT_FACTOR, 0));
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		if (noOp) return; // no execution if no-op TODO Change to NoOpAgent

		super.agentStrategy(currentTime);

		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();

		if ((bid == null && lastBid != null)
				|| (bid != null && !bid.equals(lastBid))
				|| (bid != null && lastBid == null)
				|| (ask == null && lastAsk != null)
				|| (ask != null && !ask.equals(lastAsk))
				|| (ask != null && lastAsk == null)) {	

			if (!this.getQuote().isDefined()) {
				log.log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
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
					log.log(INFO, "%s in %s: Ladder MID (%s, %s)-->(%s, %s)", this, primaryMarket, oldBid, oldAsk, bid, ask);
				}
				
				// Compute weighted moving average
				bidQueue.add(bid);
				askQueue.add(ask);
				
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
					int i = bidQueue.size()-1;
					for (Price x : bidQueue) {
						double weight = weightFactor * Math.pow(1-weightFactor, i--);
						sumBids += weight * x.intValue();
						totalWeight += weight;
					}
					i = askQueue.size()-1;
					for (Price y : askQueue) {
						double weight = weightFactor * Math.pow(1-weightFactor, i--);
						sumAsks += weight * y.intValue();
					}
				}
				Price ladderBid = new Price(sumBids / totalWeight);
				Price ladderAsk = new Price(sumAsks / totalWeight);

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