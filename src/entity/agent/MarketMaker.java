package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import com.google.common.collect.Iterators;

import systemmanager.Scheduler;
import utils.MathUtils;
import activity.SubmitOrder;
import data.FundamentalValue;
import entity.infoproc.BestBidAsk;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * Abstract class for MarketMakers.
 * 
 * All market makers submit a ladder of orders.
 * 
 * If its ladder will start exactly at where the current bid/ask
 * are, and it employs a "tick-improvement rule," it will submit a ladder with
 * the ladder mid-prices offset by 1 tick. If tickImprovement is true, it will
 * check tickInside as well.
 * 
 * Inside the quote means buy > BID, sell < ASK. Note that tickInside will be 
 * ignored if there tickImprovement is false.
 * 
 * NOTE: The MarketMaker will truncate the ladder when the price crosses
 * the NBBO, i.e., whenever one of the points in the bid would be routed to
 * the alternate market otherwise. This happens when:
 * 
 * buy orders:  If ASK_N < Y_t, then [Y_t - C_t, ..., ASK_N] (ascending)
 * sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t] (ascending)
 * 
 * @author ewah
 */
public abstract class MarketMaker extends ReentryAgent {

	private static final long serialVersionUID = -782740037969385370L;

	protected int stepSize;				// rung size is distance between adjacent rungs in ladder
	protected int numRungs;				// # of ladder rungs on one side (e.g., number of buy orders)
	protected boolean truncateLadder; 	// true if truncate if NBBO crosses ladder
	protected boolean tickImprovement;	// true if improves by a tick when mid-prices == bid/ask
	protected boolean noOp;				// FIXME, this should ne NoOpAgent instead - true if no-op strategy (never executes strategy)
	protected boolean tickInside;		// true if improve tick inside the quote (default outside)
	protected Price lastAsk, lastBid; 	// stores the last ask/bid, respectively

	public MarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			Random rand, Iterator<TimeStamp> reentry, int tickSize, 
			boolean noOp, int numRungs, int rungSize, boolean truncateLadder, 
			boolean tickImprovement, boolean tickInside) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand, reentry, tickSize);
		
		checkArgument(numRungs > 0, "Number of rungs must be positive!");
		this.noOp = noOp;
		this.numRungs = numRungs;
		this.stepSize = MathUtils.quantize(rungSize, tickSize);
		this.truncateLadder = truncateLadder;
		this.tickImprovement = tickImprovement;
		this.tickInside = tickInside;
		this.lastAsk = null;
		this.lastBid = null;
	}

	/**
	 * Shortcut constructor for exponential interarrivals (e.g. Poisson reentries)
	 */
	public MarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, double reentryRate, int tickSize, 
			boolean noOp, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickInside) {
		this(scheduler, fundamental, sip, market, rand, new ExpInterarrivals(reentryRate, rand),
				tickSize, noOp, numRungs, rungSize, truncateLadder, tickImprovement, tickInside);
	}
	
	/**
	 * Shortcut constructor for MarketMaker that only acts once
	 */
	MarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, int tickSize, 
			boolean noOp, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickInside) {
		this(scheduler, fundamental, sip, market, rand, Iterators.<TimeStamp> emptyIterator(),
				tickSize, noOp, numRungs, rungSize, truncateLadder, tickImprovement, tickInside);
	}

	/**
	 * Method to create activities for submitting a ladder of orders.
	 * 
	 * XXX Note that these are regular orders, not NMS orders.
	 * 
	 * @param buyMinPrice
	 * @param buyMaxPrice
	 * @param sellMinPrice
	 * @param sellMaxPrice
<<<<<<< HEAD
=======
	 * @param currentTime
>>>>>>> Adding tick improvement outside
	 * @return
	 */
	public void submitOrderLadder(Price buyMinPrice,
			Price buyMaxPrice, Price sellMinPrice, Price sellMaxPrice) {

		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(" in ").append(primaryMarket).append(':');

		// build ascending list of buy orders
		for (int p = buyMinPrice.intValue(); p <= buyMaxPrice.intValue(); p += stepSize) {
			scheduler.executeActivity(new SubmitOrder(this, primaryMarket, BUY, new Price(p), 1));
		}
		// build descending list of sell orders
		for (int p = sellMaxPrice.intValue(); p >= sellMinPrice.intValue(); p -= stepSize) {
			scheduler.executeActivity(new SubmitOrder(this, primaryMarket, SELL, new Price(p), 1));
		}
		log(INFO, sb.append(" Submit ladder with #rungs ").append(numRungs)
				.append(", step size ").append(stepSize).append(": buys [")
				.append(buyMinPrice).append(" to ").append(buyMaxPrice)
				.append("] & sells [").append(sellMinPrice).append(" to ")
				.append(sellMaxPrice).append("]"));
	}

	/**
	 * Given the prices (bid & ask) for the center of the ladder, compute the 
	 * ladders' prices and submit the order ladder.
	 * 
	 * If either ladderBid or ladder Ask is null, then use either lastBid or 
	 * lastAsk (or both) in lieu of the missing quote component. This will be 
	 * dealt with by truncation if lastBid/Ask will cross the current ASK/BID.
	 * 
	 * XXX MM will lose time priority if use last bid & ask, but may not be
	 * able to get around this since it doesn't know what's in the order book.
	 * 
	 * @param ladderBid
	 * @param ladderAsk
	 * @return
	 */
	public void createOrderLadder(Price ladderBid, 
			Price ladderAsk) {

		if (ladderBid == null || ladderAsk == null) return;

		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(" in ").append(primaryMarket).append(':');

		// Tick improvement
		if (this.getQuote().getBidPrice() != null) {
			Price bid = getQuote().getBidPrice();
			ladderBid = new Price(ladderBid.intValue() + 
					(bid.equals(ladderBid) && tickImprovement ? 
							(tickInside ? -1 : 1) * tickSize : 0));
		}
		if (this.getQuote().getAskPrice() != null) {
			Price ask = getQuote().getAskPrice();
			ladderAsk = new Price(ladderAsk.intValue() +  
					(ask.equals(ladderAsk) && tickImprovement ? 
							(tickInside ? 1 : -1) * tickSize: 0));
		}

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
			BestBidAsk lastNBBOQuote = this.getNBBO();

			sb.append(" Truncating ladder (").append(buyMaxPrice).append(", ")	
			.append(sellMinPrice).append(")-->(");

			// buy orders:  If ASK_N < Y_t, then [Y_t - C_t, ..., ASK_N]
			if (lastNBBOQuote.getBestAsk() != null)
				buyMaxPrice = pcomp.min(ladderBid, lastNBBOQuote.getBestAsk());
			// sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t]
			if (lastNBBOQuote.getBestBid() != null)
				sellMinPrice = pcomp.max(ladderAsk, lastNBBOQuote.getBestBid());
			log(INFO, sb.append(buyMaxPrice).append(", ").append(sellMinPrice)
					.append(")"));
		}

		this.submitOrderLadder(buyMinPrice, buyMaxPrice, sellMinPrice,
				sellMaxPrice);
	}
}
