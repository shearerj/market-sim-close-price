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
import entity.market.Quote;
import event.TimeStamp;

/**
 * MAMARKETMAKER
 * 
 * Moving Average Market Maker
 * 
 * @author zzy, ewah
 */
public class MAMarketMaker extends MarketMaker {

	private static final long serialVersionUID = -4766539518925397355L;
	
	protected EvictingQueue<Price> bidQueue;
	protected EvictingQueue<Price> askQueue;
	
	public MAMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, double reentryRate, int tickSize, boolean noOp,
			int numRungs, int rungSize, boolean truncateLadder, int numHistorical) {
		super(fundamental, sip, market, rand, reentryRate, tickSize, noOp, 
				numRungs, rungSize, truncateLadder);

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
				props.getAsInt(Keys.NUM_HISTORICAL, 5));
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
				// do nothing, wait until next re-entry
			} else {
				lastNBBOQuote = sip.getNBBO();
				bid = marketQuoteProcessor.getQuote().getBidPrice();
				ask = marketQuoteProcessor.getQuote().getAskPrice();
				
				bidQueue.add(bid);
				askQueue.add(ask);
				
				// Compute moving average
				double sumBids = 0;
				for (Price x : bidQueue) sumBids += x.intValue();
				Price ladderBid = new Price(sumBids / bidQueue.size());
				
				double sumAsks = 0;
				for (Price y : askQueue) sumAsks += y.intValue();
				Price ladderAsk = new Price(sumAsks / askQueue.size());

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
		return "MAMarketMaker " + super.toString();
	}
}

