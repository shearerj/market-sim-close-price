package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import activity.Activity;
import activity.WithdrawOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.BestBidAsk;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

public class WMAMarketMaker extends MarketMaker {

	private static final long serialVersionUID = -8566264088391504213L;
	
	private int histosize;
	private double weightFactor;	// for computing weighted MA
	private Queue<Price> histobids;
	private Queue<Price> histoasks;
	
	protected WMAMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, double reentryRate, int tickSize, boolean noOp,
			int numRungs, int rungSize, boolean truncateLadder, int windowLength, 
			double weightFactor) {
		super(fundamental, sip, market, rand, reentryRate, tickSize, noOp, 
				numRungs, rungSize, truncateLadder);

		histosize = windowLength;
		this.weightFactor = weightFactor;
		histobids = new LinkedList<Price>(); // XXX change to Guava EvictingQueue
		histoasks = new LinkedList<Price>();
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
				props.getAsDouble(Keys.WEIGHT_FACTOR, 0.9));
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
			for (Order order : activeOrders)
				acts.add(new WithdrawOrder(order, TimeStamp.IMMEDIATE));

			if (!quote.isDefined()) {
				log(INFO, this + " " + getName()
						+ "::agentStrategy: undefined quote in market "
						+ primaryMarket);
			} else {
				if (histobids.size() < histosize) {
					histobids.add(bid);
					histoasks.add(ask);
					// XXX use all same bid/ask to slowly fill in / transition
					
				} else {
					// get bid and ask price by averaging the 10 time stamp historical prices
					int totalbids = 0;
					int totalasks = 0;
					int weight = 0;
					//assume the prices in historical prices are loaded in time sequence
					int i = 0;
					for (Price x : histobids) {
						totalbids = totalbids + (++i)*x.intValue();
						weight = weight+i;
					}
					i = 0;
					for (Price y : histoasks){
						totalasks = totalasks + (++i)*y.intValue();
					}
					Price ladderbid = new Price(totalbids/weight);
					Price ladderask = new Price(totalasks/weight);
				
					histobids.poll();
					histoasks.poll();
		
					histobids.add(bid);
					histoasks.add(ask);

					int ct = (numRungs-1) * stepSize;

					// min price for buy order in the ladder
					Price buyMinPrice = new Price(ladderbid.intValue() - ct);
					// max price for buy order in the ladder
					Price buyMaxPrice = ladderbid;

					// min price for sell order in the ladder
					Price sellMinPrice = ladderask;
					// max price for sell order in the ladder
					Price sellMaxPrice = new Price(ladderask.intValue() + ct);
					
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
				}
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