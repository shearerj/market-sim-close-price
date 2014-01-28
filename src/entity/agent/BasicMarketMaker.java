package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

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

	public BasicMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			Random rand, double reentryRate, int tickSize, boolean noOp,
			int numRungs, int rungSize, boolean truncateLadder, 
			boolean tickImprovement) {
		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize, noOp, 
				numRungs, rungSize, truncateLadder, tickImprovement);	
	}
	
	/**
	 * Shortcut constructor for agent that doesn't reenter.
	 */
	BasicMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			Random rand, int tickSize, boolean noOp,
			int numRungs, int rungSize, boolean truncateLadder, 
			boolean tickImprovement) {
		super(scheduler, fundamental, sip, market, rand, tickSize, noOp, 
				numRungs, rungSize, truncateLadder, tickImprovement);	
	}

	public BasicMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			Random rand, EntityProperties props) {
		this(scheduler, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.0005),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsBoolean(Keys.NO_OP, false),
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000), 
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true), 
				props.getAsBoolean(Keys.TICK_IMPROVEMENT, true));
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		if (noOp) return; // no execution if no-op TODO Change to NoOpAgent

		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(" in ").append(primaryMarket).append(':');
		
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
				log(INFO, sb.append(" Undefined quote in ").append(primaryMarket));
			} else {
				// Quote changed, still valid, withdraw all orders
				log(INFO, sb.append(" Withdraw all orders."));
				withdrawAllOrders();
				
				bid = this.getQuote().getBidPrice();
				ask = this.getQuote().getAskPrice();
				
				// Use last known bid/ask if undefined post-withdrawal
				if (!this.getQuote().isDefined()) {
					sb.append(" Ladder MID (").append(bid).append(", ")
							.append(ask).append(")-->(");
					if (bid == null && lastBid != null) bid = lastBid;
					if (ask == null && lastAsk != null) ask = lastAsk;
					log(INFO, sb.append(bid).append(", ").append(ask).append(")"));
				}
				
				this.createOrderLadder(bid, ask);
			}
		} else {
			log(INFO, sb.append(" No change in submitted ladder"));
		}
		// update latest bid/ask prices
		lastAsk = ask; lastBid = bid;
	}

}
