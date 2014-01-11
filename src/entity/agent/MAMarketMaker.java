package entity.agent;

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
import entity.infoproc.BestBidAsk;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

public class MAMarketMaker extends MarketMaker {

	private static final long serialVersionUID = -4766539518925397355L;
	
	protected int numElements;					// number of prices to average
	protected EvictingQueue<Price> bidQueue;
	protected EvictingQueue<Price> askQueue;
	
	public MAMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, double reentryRate, int tickSize, boolean noOp,
			int numRungs, int rungSize, boolean truncateLadder, int windowLength) {
		super(fundamental, sip, market, rand, reentryRate, tickSize, noOp, 
				numRungs, rungSize, truncateLadder);

		numElements = windowLength;
		bidQueue = EvictingQueue.create(windowLength);
		askQueue = EvictingQueue.create(windowLength);
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
				props.getAsInt(Keys.WINDOW_LENGTH, 5));
	}
	
	@Override
	public Iterable<Activity> agentStrategy(TimeStamp currentTime) {
		if (noOp) return ImmutableList.of(); // no execution if no-op
		
		Builder<Activity> acts = ImmutableList.<Activity> builder().addAll(
				super.agentStrategy(currentTime));
		
		BestBidAsk lastNBBOQuote = sip.getNBBO();

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
			this.withdrawAllOrders(currentTime);

			if (!quote.isDefined()) {
				log(INFO, this + " " + getName()
						+ "::agentStrategy: undefined quote in market "
						+ primaryMarket);
				// do nothing, wait until next re-entry
			} else {
				
				if (bidQueue.isEmpty()) {
					// if queues are empty, fill in all with the same bid/ask
					for (int i = 0; i < numElements; i++) {
						bidQueue.add(bid);
						askQueue.add(ask);
					}
				}
				bidQueue.add(bid);
				askQueue.add(ask);
				
				// Compute moving average
				double sumBids = 0;
				for (Price x : bidQueue) sumBids += x.intValue();
				
				double sumAsks = 0;
				for (Price y : askQueue) sumAsks += y.intValue();
				
				Price ladderBid = new Price(sumBids / numElements);
				Price ladderAsk = new Price(sumAsks / numElements);

				bidQueue.add(bid);
				askQueue.add(ask);

				int ct = (numRungs-1) * stepSize;

				// min price for buy order in the ladder
				Price buyMinPrice = new Price(ladderBid.intValue() - ct);
				// max price for buy order in the ladder
				Price buyMaxPrice = ladderBid;

				// min price for sell order in the ladder
				Price sellMinPrice = ladderAsk;
				// max price for sell order in the ladder
				Price sellMaxPrice = new Price(ladderAsk.intValue() + ct);

				// check if the bid or ask crosses the NBBO, if truncating ladder
				if (truncateLadder) {
					// buy orders:  If ASK_N < Y_t, then [Y_t - C_t, ..., ASK_N]
					buyMaxPrice = pcomp.min(bid, lastNBBOQuote.getBestAsk());
					// sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t]
					sellMinPrice = pcomp.max(ask, lastNBBOQuote.getBestBid());
				}

				// TODO if matches bid, then go one tick in				
				acts.addAll(this.submitOrderLadder(buyMinPrice, buyMaxPrice, 
						sellMinPrice, sellMaxPrice, currentTime));

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

