package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * BASICMARKETMAKER
 * 
 * Basic market maker. See description in (Chakraborty & Kearns, 2011).
 * Participates in only a single market at a time. Submits a ladder of bids
 * based on BID = Y_t < X_t = ASK, where C_t = numRungs * stepSize:
 * 
 * buy orders:  [Y_t - C_t, ..., Y_t - C_1, Y_t]
 * sell orders: [X_t, X_t + C_1, ..., X_t + C_t]
 * 
 * The market maker liquidates its position at the price dictated by the
 * global fundamental at the end of the simulation.
 * 
 * The market maker will only submit a ladder if both the bid and the ask
 * are defined.
 * 
 * If, after it withdraws its orders, it notices that the quote is now 
 * undefined, it will submit a ladder with either lastBid or lastAsk (or
 * both) in lieu of the missing quote component. Notice that if
 * lastBid/Ask crosses the current ASK/BID, truncation will handle this.
 * 
 * XXX MM will lose time priority if use last bid & ask, but may not be
 * able to get around this
 * 
 * @author ewah
 */
public class BasicMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 9057600979711100221L;

	public BasicMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			int tickSize, boolean noOp, int numRungs, int rungSize,
			boolean truncateLadder, boolean tickImprovement, boolean tickInside, 
			int initLadderMean, int initLadderRange) {

		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				noOp, numRungs, rungSize, truncateLadder, tickImprovement,
				tickInside, initLadderMean, initLadderRange);
	}

	public BasicMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
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
				props.getAsInt(Keys.INITIAL_LADDER_MEAN, 0),
				props.getAsInt(Keys.INITIAL_LADDER_RANGE, 0));
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		if (noOp) return; // no execution if no-op TODO Change to NoOpAgent

		super.agentStrategy(currentTime);

		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();

		if (bid == null && lastBid == null && ask == null && lastAsk == null) {
			this.createOrderLadder(bid, ask);	
		}
		else if ((bid == null && lastBid != null)
				|| (bid != null && !bid.equals(lastBid))
				|| (bid != null && lastBid == null)
				|| (ask == null && lastAsk != null)
				|| (ask != null && !ask.equals(lastAsk))
				|| (ask != null && lastAsk == null)) {

			if (!this.getQuote().isDefined()) {
				log.log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
			} else {
				// Quote changed, still valid, withdraw all orders
				log.log(INFO, "%s in %s: Withdraw all orders.", this, primaryMarket);
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
				
				this.createOrderLadder(bid, ask);
			} // if quote defined
		} else {
			log.log(INFO, "%s in %s: No change in submitted ladder", this, primaryMarket);
		}
		// update latest bid/ask prices
		lastAsk = ask; lastBid = bid;
	}

}
