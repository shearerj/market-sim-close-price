package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import utils.MathUtils;
import activity.Activity;
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
 * @author ewah
 */
public abstract class MarketMaker extends ReentryAgent {

	private static final long serialVersionUID = -782740037969385370L;
	
	protected int stepSize;	// rung size is distance between adjacent rungs in ladder
	protected int numRungs;	// # of ladder rungs on one side (e.g., number of buy orders)
	protected boolean truncateLadder; 	// true if truncate if NBBO crosses ladder
	protected boolean noOp;	// true if no-op strategy (never executes strategy)
	
	protected Price lastAsk, lastBid; // stores the last ask/bid, respectively
	protected BestBidAsk lastNBBOQuote;

	public MarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, Iterator<TimeStamp> reentry, int tickSize, 
			boolean noOp, int numRungs, int rungSize, boolean truncateLadder) {
		super(TimeStamp.ZERO, fundamental, sip, market, rand, reentry, tickSize);
		checkArgument(numRungs > 0, "Number of rungs must be positive!");
		this.noOp = noOp;
		this.numRungs = numRungs;
		this.stepSize = MathUtils.quantize(rungSize, tickSize);
		this.truncateLadder = truncateLadder;
		this.lastAsk = null;
		this.lastBid = null;
	
	}
	
	/**
	 * Shortcut constructor for exponential interarrivals (e.g. Poisson reentries)
	 */
	public MarketMaker(FundamentalValue fundamental, SIP sip,
			Market market, Random rand, double reentryRate, int tickSize, 
			boolean noOp, int numRungs, int rungSize, boolean truncateLadder) {
		this(fundamental, sip, market, rand, new ExpInterarrivals(reentryRate, rand),
				tickSize, noOp, numRungs, rungSize, truncateLadder);
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
	 * @param currentTime
	 * 
	 * @return
	 */
	public Iterable<? extends Activity> submitOrderLadder(Price buyMinPrice,
			Price buyMaxPrice, Price sellMinPrice, Price sellMaxPrice, 
			TimeStamp currentTime) {		
		Builder<Activity> acts = ImmutableList.<Activity> builder();

		// build ascending list of buy orders
		for (int p = buyMinPrice.intValue(); p <= buyMaxPrice.intValue(); p += stepSize) {
			acts.add(new SubmitOrder(this, primaryMarket, BUY, new Price(p), 1, TimeStamp.IMMEDIATE));
		}
		// build descending list of sell orders
		for (int p = sellMaxPrice.intValue(); p >= sellMinPrice.intValue(); p -= stepSize) { 
			acts.add(new SubmitOrder(this, primaryMarket, SELL, new Price(p), 1, TimeStamp.IMMEDIATE));
		}
		
		log(INFO, primaryMarket + " " + this + " " + getName()
				+ "::agentStrategy: ladder numRungs=" + numRungs
				+ ", stepSize=" + stepSize + ": buys [" + buyMinPrice
				+ ", " + buyMaxPrice + "] &" + " sells [" + sellMinPrice + ", "
				+ sellMaxPrice + "]");
		
		return acts.build();
	}
	
	/**
	 * Given the prices (bid & ask) for the center of the ladder, compute the 
	 * ladders prices and submit the order ladder.
	 * 
	 * @param ladderBid
	 * @param ladderAsk
	 * @param currentTime
	 * @return
	 */
	public Iterable<? extends Activity> createOrderLadder(Price ladderBid, Price ladderAsk,
			TimeStamp currentTime) {
		Builder<Activity> acts = ImmutableList.<Activity> builder();
		
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
			buyMaxPrice = pcomp.min(ladderBid, lastNBBOQuote.getBestAsk());
			// sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t]
			sellMinPrice = pcomp.max(ladderAsk, lastNBBOQuote.getBestBid());
		}
		
		// TODO if matches bid, then go one tick in				
		acts.addAll(this.submitOrderLadder(buyMinPrice, buyMaxPrice, 
				sellMinPrice, sellMaxPrice, currentTime));
		return acts.build();
	}
}
