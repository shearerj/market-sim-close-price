package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static data.Observations.BUS;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;
import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Scheduler;
import utils.MathUtils;
import utils.Rands;
import activity.SubmitOrder;
import data.FundamentalValue;
import data.Observations.MarketMakerStatistic;
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
 * ignored if tickImprovement is false.
 * 
 * Added March 22, 2014: Market makers will randomly initialize a ladder given 
 * a meanLadderCenter and rangeLadderCenter (this will ensure ladder center is
 * uniformly random in the range of specified length centered around the mean),
 * and the spread of the ladder center is given by the ladder stepSize.
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
	protected boolean tickOutside;		// true if improve tick outside the quote (default inside, bid<p<ask)
	protected Price lastAsk, lastBid; 	// stores the last ask/bid, respectively
	protected int initLadderMean;		// for initializing ladder center
	protected int initLadderRange;		// for initializing ladder center
	
	public MarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, Iterator<TimeStamp> reentry,
			int tickSize, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickOutside, int initLadderMean, 
			int initLadderRange) {
		
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand,
				reentry, tickSize);

		checkArgument(numRungs > 0, "Number of rungs must be positive!");
		this.numRungs = numRungs;
		this.stepSize = MathUtils.quantize(rungSize, tickSize);
		this.truncateLadder = truncateLadder;
		this.tickImprovement = tickImprovement;
		this.tickOutside = tickOutside;
		this.lastAsk = null;
		this.lastBid = null;
		checkArgument(initLadderMean >= 0, "Ladder initialization mean must be positive!");
		checkArgument(initLadderRange >= 0, "Ladder initialization range must be positive!");
		this.initLadderMean = initLadderMean;
		this.initLadderRange = initLadderRange;
		if (initLadderRange > 0 && initLadderMean == 0) {
			// initialize using mean fundamental value if ladder range defined
			 this.initLadderMean = fundamental.getMeanValue();
		}
	}

	/**
	 * Shortcut constructor for exponential interarrivals (e.g. Poisson reentries)
	 */
	public MarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, double reentryRate, int tickSize, 
			int numRungs, int rungSize, boolean truncateLadder, boolean tickImprovement,
			boolean tickInside, int initLadderMean, int initLadderRange) {

		this(scheduler, fundamental, sip, market, rand, ExpInterarrivals.create(reentryRate, rand),
				tickSize, numRungs, rungSize, truncateLadder, tickImprovement, 
				tickInside, initLadderMean, initLadderRange);
	}

	/**
	 * Method to create activities for submitting a ladder of orders.
	 * 
	 * XXX Note that these are regular orders, not NMS orders.
	 * 
	 * buyMin < buyMax < sellMin < sellMax
	 * 
	 * @param buyMinPrice
	 * @param buyMaxPrice
	 * @param sellMinPrice
	 * @param sellMaxPrice
	 * @return
	 */
	public void submitOrderLadder(Price buyMinPrice, Price buyMaxPrice, 
			Price sellMinPrice, Price sellMaxPrice) {

		// build ascending list of buy orders
		for (int p = buyMinPrice.intValue(); p <= buyMaxPrice.intValue(); p += stepSize) {
			scheduler.executeActivity(new SubmitOrder(this, primaryMarket, BUY, new Price(p), 1));
		}
		// build descending list of sell orders
		for (int p = sellMaxPrice.intValue(); p >= sellMinPrice.intValue(); p -= stepSize) {
			scheduler.executeActivity(new SubmitOrder(this, primaryMarket, SELL, new Price(p), 1));
		}
		log(INFO, "%s in %s: Submit ladder with #rungs %d, step size %d: buys [%s to %s] & sells [%s to %s]",
				this, primaryMarket, numRungs, stepSize, buyMinPrice, buyMaxPrice, sellMinPrice, sellMaxPrice);
		
		BUS.post(new MarketMakerStatistic(this, buyMaxPrice, sellMinPrice));
		// XXX for evaluating strategies
		// System.out.println((buyMaxPrice.doubleValue() + sellMinPrice.doubleValue())/2 + "," + 
		//		(sellMinPrice.intValue() - buyMaxPrice.intValue()));
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
	public void createOrderLadder(Price ladderBid, Price ladderAsk) {

		if (ladderBid == null || ladderAsk == null) {
			if (initLadderMean == 0) return;
			else {
				if (ladderBid != null && ladderAsk == null) {
					if (initLadderRange > 2 * stepSize) {
						ladderAsk = new Price(Rands.nextUniform(rand, ladderBid.intValue() + 2 * stepSize, 
								ladderBid.intValue() + initLadderRange));
					} else {
						ladderAsk = new Price(Rands.nextUniform(rand, ladderBid.intValue() + initLadderRange, 
								ladderBid.intValue() + 2 * stepSize));
					}
					
				} else if (ladderBid == null && ladderAsk != null) {
					if (initLadderRange > 2 * stepSize) {
						ladderBid = new Price(Rands.nextUniform(rand, ladderAsk.intValue() - 2 * stepSize, 
								ladderAsk.intValue() - initLadderRange));
					} else {
						ladderBid = new Price(Rands.nextUniform(rand, ladderAsk.intValue() - initLadderRange, 
								ladderAsk.intValue() - 2 * stepSize));
					}
					
				} else if (ladderBid == null && ladderAsk == null) {
					// initialize ladder prices
					double ladderMeanMin = initLadderMean - initLadderRange;
					double ladderMeanMax = initLadderMean + initLadderRange;
					int ladderCenter = (int) Rands.nextUniform(rand, ladderMeanMin, ladderMeanMax);
					ladderBid = new Price(ladderCenter - stepSize);
					ladderAsk = new Price(ladderCenter + stepSize);
				}
				log(INFO, "%s in %s: Randomized Ladder MID (%s, %s)", 
						this, primaryMarket, ladderBid, ladderAsk);
			}
		}

		// Tick improvement
		if (this.getQuote().getBidPrice() != null) {
			Price bid = getQuote().getBidPrice();
			ladderBid = new Price(ladderBid.intValue() + 
					(bid.equals(ladderBid) && tickImprovement ? 
							(tickOutside ? -1 : 1) * tickSize : 0));
		}
		if (this.getQuote().getAskPrice() != null) {
			Price ask = getQuote().getAskPrice();
			ladderAsk = new Price(ladderAsk.intValue() +  
					(ask.equals(ladderAsk) && tickImprovement ? 
							(tickOutside ? 1 : -1) * tickSize: 0));
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
			Price oldBuyMaxPrice = buyMaxPrice, oldSellMinPrice = sellMinPrice;
			// buy orders:  If ASK_N < Y_t, then [Y_t - C_t, ..., ASK_N]
			if (lastNBBOQuote.getBestAsk() != null)
				buyMaxPrice = pcomp.min(ladderBid, lastNBBOQuote.getBestAsk());
			// sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t]
			if (lastNBBOQuote.getBestBid() != null)
				sellMinPrice = pcomp.max(ladderAsk, lastNBBOQuote.getBestBid());
			log(INFO, "%s in %s: Truncating ladder(%s, %s)-->(%s, %s)", 
					this, primaryMarket, oldBuyMaxPrice, oldSellMinPrice, buyMaxPrice, sellMinPrice);
		}

		this.submitOrderLadder(buyMinPrice, buyMaxPrice, sellMinPrice,
				sellMaxPrice);
	}

	@Override
	protected String name() {
		String oldName = super.name();
		return oldName.substring(0, oldName.length() - 6) + "MM";
	}
	
}
