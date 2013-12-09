package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static fourheap.Order.OrderType.*;

import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import systemmanager.Keys;
import utils.MathUtils;
import activity.Activity;
import activity.SubmitOrder;
import activity.WithdrawOrder;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.BestBidAsk;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

/**
 * BasicMarketMaker
 * 
 * Basic market maker. See description in (Chakraborty & Kearns, 2011).
 * Participates in only a single market at a time. Submits a ladder of bids
 * based on BID = Y_t < X_t = ASK, where C_t = numRungs * stepSize:
 * 
 * buy orders:  [Y_t - C_t, ..., Y_t - C_1, Y_t]
 * sell orders: [X_t, X_t + C_1, ..., X_t + C_t]
 * 
 * The market maker liquidates its position at the price dictated by the
 * global fundamental at the end of the simulation (event is inserted in 
 * SystemSetup).
 * 
 * NOTE: The MarketMakerAgent will truncate the ladder when the price crosses
 * the NBBO, i.e., whenever one of the points in the bid would be routed to
 * the alternate market otherwise. This happens when:
 * 
 * buy orders:  If ASK_N < Y_t, then [Y_t - C_t, ..., ASK_N] (ascending)
 * sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t] (ascending)
 * 
 * @author ewah
 */
public class BasicMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 9057600979711100221L;
	
	private int stepSize;	// rung size is distance between adjacent rungs in ladder
	private int numRungs;	// # of ladder rungs on one side (e.g., number of buy orders)
	private boolean truncateLadder; 	// true if truncate if NBBO crosses ladder
	protected Price lastAsk, lastBid; // stores the last ask/bid, respectively

	public BasicMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, int numRungs, int rungSize, double reentryRate,
			boolean truncateLadder, int tickSize) {
		super(fundamental, sip, market, rand, reentryRate, tickSize);
		this.numRungs = numRungs;
		this.stepSize = MathUtils.quantize(rungSize, tickSize);
		this.truncateLadder = truncateLadder;
		this.lastAsk = null;
		this.lastBid = null;
	}

	public BasicMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, EntityProperties props) {
		this(fundamental, sip, market, rand,
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000),
				props.getAsDouble(Keys.REENTRY_RATE, 0.0005),
				props.getAsBoolean(Keys.TRUNCATE_LADDER_KEY, true),
				props.getAsInt(Keys.TICK_SIZE, 1));
	}

	@Override
	public Iterable<Activity> agentStrategy(TimeStamp currentTime) {
		// TODO how to check if no-op strategy here...
		
		Builder<Activity> acts = ImmutableList.<Activity> builder().addAll(
				super.agentStrategy(currentTime));

		// update NBBO
		BestBidAsk lastNBBOQuote = sip.getNBBO();

		Quote quote = marketQuoteProcessor.getQuote();
		Price bid = quote.getBidPrice();
		Price ask = quote.getAskPrice();
		
		// Quote changed, withdraw all orders
		if ((bid == null && lastBid != null)
				|| (bid != null && !bid.equals(lastBid))
				|| (ask == null && lastAsk != null)
				|| (ask != null && !ask.equals(lastAsk))) {
			for (Order order : activeOrders)
				acts.add(new WithdrawOrder(order, TimeStamp.IMMEDIATE));
			
			if (!quote.isDefined()) {
				log(INFO, this + " " + getName()
						+ "::agentStrategy: undefined quote in market "
						+ primaryMarket);
			} else {
				
				int ct = (numRungs-1) * stepSize;
				
				// min price for buy order in the ladder
				Price buyMinPrice = new Price(bid.intValue() - ct);
				// max price for buy order in the ladder
				Price buyMaxPrice = bid;
				
				// min price for sell order in the ladder
				Price sellMinPrice = ask;
				// max price for sell order in the ladder
				Price sellMaxPrice = new Price(ask.intValue() + ct);
				
				// check if the bid or ask crosses the NBBO, if truncating ladder
				if (truncateLadder) {
					// buy orders:  If ASK_N < Y_t, then [Y_t - C_t, ..., ASK_N]
					buyMaxPrice = pcomp.min(bid, lastNBBOQuote.getBestAsk());
					// sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t]
					sellMinPrice = pcomp.max(ask, lastNBBOQuote.getBestBid());
				}
				
				// check if the bid or ask crosses the NBBO
				if (lastNBBOQuote.getBestAsk().lessThan(ask)) {
					// buy orders: If ASK_N < X_t, then [ASK_N, ..., Y_t]
					buyMaxPrice = lastNBBOQuote.getBestAsk();
				}
				if (lastNBBOQuote.getBestBid().greaterThan(bid)) {
					// sell orders: If BID_N > Y_t, then [X_t, ..., BID_N]
					sellMinPrice = lastNBBOQuote.getBestBid();
				}

				// submits only one side if either bid or ask is undefined
				if (bid != null) {
					// build ascending list of buy orders
					for (int p = buyMinPrice.intValue(); p <= buyMaxPrice.intValue(); p += stepSize) {
						acts.add(new SubmitOrder(this, primaryMarket, BUY, new Price(p), 1, TimeStamp.IMMEDIATE));
					}
				}
				if (ask != null) {
					// build descending list of sell orders
					for (int p = sellMaxPrice.intValue(); p >= sellMinPrice.intValue(); p -= stepSize) { 
						acts.add(new SubmitOrder(this, primaryMarket, SELL, new Price(p), 1, TimeStamp.IMMEDIATE));
					}
				}
				
				log(INFO, primaryMarket + " " + this + " " + getName()
						+ "::agentStrategy: ladder numRungs=" + numRungs
						+ ", stepSize=" + stepSize + ": buys [" + buyMinPrice
						+ ", " + buyMaxPrice + "] &" + " sells [" + sellMinPrice + ", "
						+ sellMaxPrice + "]");
				
			}
		} else {
			log(INFO, currentTime + " | " + primaryMarket + " " + this + " " + getName()
					+ "::agentStrategy: no change in submitted ladder.");
		}
		// update latest bid/ask prices
		lastAsk = ask;
		lastBid = bid;

		return acts.build();
	}
}
