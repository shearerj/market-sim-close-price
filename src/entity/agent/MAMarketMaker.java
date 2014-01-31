package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Random;

import activity.Activity;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price; 	
import event.TimeStamp;

/**
 * MAMARKETMAKER
 * 
 * Moving Average Market Maker
 * 
 * NOTE: Because the prices are stored in an EvictingQueue, which does not
 * accept null elements, the number of elements in the bid/ask queues may not
 * be equivalent.
 * 
 * @author zzy, ewah
 */
public class MAMarketMaker extends MarketMaker {

	private static final long serialVersionUID = -4766539518925397355L;
	
	protected EvictingQueue<Price> bidQueue;
	protected EvictingQueue<Price> askQueue;
	
	public MAMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, double reentryRate, int tickSize, boolean noOp,
			int numRungs, int rungSize, boolean truncateLadder, 
			boolean tickImprovement, boolean tickInside, int numHistorical) {
		super(fundamental, sip, market, rand, reentryRate, tickSize, noOp, 
				numRungs, rungSize, truncateLadder, tickImprovement, tickInside);

		checkArgument(numHistorical > 0, "Number of historical prices must be positive!");
		bidQueue = EvictingQueue.create(numHistorical);
		askQueue = EvictingQueue.create(numHistorical);
	}
	
	public MAMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, EntityProperties props) {
		this(fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.0005),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsBoolean(Keys.NO_OP, false),
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000), 
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true),
				props.getAsBoolean(Keys.TICK_IMPROVEMENT, true),
				props.getAsBoolean(Keys.TICK_INSIDE, true),
				props.getAsInt(Keys.NUM_HISTORICAL, 5));
	}
	
	@Override
	public Iterable<Activity> agentStrategy(TimeStamp currentTime) {
		if (noOp) return ImmutableList.of(); // no execution if no-op
		
		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(" in ").append(primaryMarket).append(':');
		
		Builder<Activity> acts = ImmutableList.<Activity> builder().addAll(
				super.agentStrategy(currentTime));
		
		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();;

		// Quote changed, withdraw all orders
		if ((bid == null && lastBid != null)
				|| (bid != null && !bid.equals(lastBid))
				|| (bid != null && lastBid == null)
				|| (ask == null && lastAsk != null)
				|| (ask != null && !ask.equals(lastAsk))
				|| (ask != null && lastAsk == null)) {
			
			if (!this.getQuote().isDefined()) {
				log(INFO, sb.append(" Undefined quote in ").append(primaryMarket));
				// do nothing, wait until next re-entry
			} else {
				// Quote changed, still valid, withdraw all orders
				log(INFO, sb.append(" Withdraw all orders"));
				acts.addAll(withdrawAllOrders(currentTime));	
				
				bid = this.getQuote().getBidPrice();
				ask = this.getQuote().getAskPrice();
				
				// Use last known bid/ask if undefined post-withdrawal
				if (!this.getQuote().isDefined()) {
					sb.append(" Ladder MID (").append(bid).append(",")
						.append(ask).append(")-->(");
					if (bid == null && lastBid != null) bid = lastBid;
					if (ask == null && lastAsk != null) ask = lastAsk;
					log(INFO, sb.append(bid).append(",").append(ask).append(")"));
				}
				
				// Compute moving average
				bidQueue.add(bid);
				askQueue.add(ask);
				
				double sumBids = 0;
				for (Price x : bidQueue) sumBids += x.intValue();
				Price ladderBid = new Price(sumBids / bidQueue.size());
				
				double sumAsks = 0;
				for (Price y : askQueue) sumAsks += y.intValue();
				Price ladderAsk = new Price(sumAsks / askQueue.size());

				acts.addAll(this.createOrderLadder(ladderBid, ladderAsk, currentTime));

			} // if quote defined
		} else {
			log(INFO, sb.append(" No change in submitted ladder"));
		}
		// update latest bid/ask prices
		lastAsk = ask;
		lastBid = bid;

		return acts.build();
	}
	
	@Override
	public String toString() {
		return "MAMM " + super.toString();
	}
}

