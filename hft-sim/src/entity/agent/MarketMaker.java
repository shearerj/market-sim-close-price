package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.InitLadderMean;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.MarketMakerReentryRate;
import systemmanager.Keys.NumRungs;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.RungSize;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TickOutside;
import systemmanager.Keys.TruncateLadder;
import systemmanager.Simulation;
import utils.Maths;
import utils.Rands;

import com.google.common.base.Optional;
import com.google.common.collect.Ordering;

import data.Props;
import data.Stats;
import entity.agent.position.PrivateValues;
import entity.infoproc.BestBidAsk;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;
import fourheap.Order.OrderType;

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
	
	private static final Ordering<Price> pcomp = Ordering.natural();

	protected final int stepSize;				// rung size is distance between adjacent rungs in ladder
	protected final int numRungs;				// # of ladder rungs on one side (e.g., number of buy orders)
	protected final int initLadderMean;			// for initializing ladder center
	protected final int initLadderRange; 		// for initializing ladder center
	protected final boolean truncateLadder; 	// true if truncate if NBBO crosses ladder
	protected final boolean tickImprovement;	// true if improves by a tick when mid-prices == bid/ask
	protected final boolean tickOutside;		// true if improve tick outside the quote (default inside, bid<p<ask)
	
	protected MarketMaker(Simulation sim, Market market, Random rand, Props props) {		
		super(sim, PrivateValues.zero(), TimeStamp.ZERO, market, rand,
				AgentFactory.exponentials(props.get(MarketMakerReentryRate.class, ReentryRate.class), rand),
				props);
		
		this.numRungs = props.get(NumRungs.class);
		this.stepSize = Maths.quantize(props.get(RungSize.class), tickSize);
		this.truncateLadder = props.get(TruncateLadder.class);
		this.tickImprovement = props.get(TickImprovement.class);
		this.tickOutside = props.get(TickOutside.class);
		this.initLadderRange = props.get(InitLadderRange.class, FundamentalMean.class);
		int tempLadderMean = props.get(InitLadderMean.class, FundamentalMean.class);
		
		checkArgument(numRungs > 0, "Number of rungs must be positive!");
		checkArgument(tempLadderMean >= 0, "Ladder initialization mean must be positive!");
		checkArgument(initLadderRange >= 0, "Ladder initialization range must be positive!");
		
		// initialize using mean fundamental value if ladder range defined
		// XXX Cheating? Also, this could probably be done better?
		this.initLadderMean = initLadderRange > 0 && tempLadderMean == 0 ? props.get(FundamentalMean.class) : tempLadderMean;
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
		for (int p = buyMinPrice.intValue(); p <= buyMaxPrice.intValue(); p += stepSize)
			submitOrder(BUY, Price.of(p), 1);
		// build descending list of sell orders
		for (int p = sellMaxPrice.intValue(); p >= sellMinPrice.intValue(); p -= stepSize)
			submitOrder(SELL, Price.of(p), 1);

		log(INFO, "%s in %s: Submit ladder with #rungs %d, step size %d: buys [%s to %s] & sells [%s to %s]",
				this, primaryMarket, numRungs, stepSize, buyMinPrice, buyMaxPrice, sellMinPrice, sellMaxPrice);
		
		// For evaluating strategies
		postStat(Stats.MARKET_MAKER_SPREAD + this, sellMinPrice.doubleValue() - buyMaxPrice.doubleValue());
		postStat(Stats.MARKET_MAKER_LADDER + this, (buyMaxPrice.doubleValue() + sellMinPrice.doubleValue())/2);
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
	 * @param initLadderBid
	 * @param initLadderAsk
	 * @return
	 */
	public void createOrderLadder(Optional<Price> initLadderBid, Optional<Price> initLadderAsk) {
		if ((!initLadderAsk.isPresent() || !initLadderBid.isPresent()) && initLadderMean == 0)
			return;
		Price ladderBid, ladderAsk;
		if (initLadderBid.isPresent()) {
			ladderBid = initLadderBid.get();
			if (initLadderAsk.isPresent()) {
				ladderAsk = initLadderAsk.get();
			} else {
				if (initLadderRange > 2 * stepSize) {
					ladderAsk = Price.of(Rands.nextUniform(rand, initLadderBid.get().intValue() + 2 * stepSize, 
							initLadderBid.get().intValue() + initLadderRange));
				} else {
					ladderAsk = Price.of(Rands.nextUniform(rand, initLadderBid.get().intValue() + initLadderRange, 
							initLadderBid.get().intValue() + 2 * stepSize));
				}
				log(INFO, "%s in %s: Randomized Ladder MID (%s, %s)", 
						this, primaryMarket, initLadderBid, initLadderAsk);
			}
		} else {
			if (initLadderAsk.isPresent()) {
				ladderAsk = initLadderAsk.get();
				if (initLadderRange > 2 * stepSize) {
					ladderBid = Price.of(Rands.nextUniform(rand, initLadderAsk.get().intValue() - 2 * stepSize, 
							initLadderAsk.get().intValue() - initLadderRange));
				} else {
					ladderBid = Price.of(Rands.nextUniform(rand, initLadderAsk.get().intValue() - initLadderRange, 
							initLadderAsk.get().intValue() - 2 * stepSize));
				}
			} else {
				double ladderMeanMin = initLadderMean - initLadderRange;
				double ladderMeanMax = initLadderMean + initLadderRange;
				int ladderCenter = (int) Rands.nextUniform(rand, ladderMeanMin, ladderMeanMax);
				ladderBid = Price.of(ladderCenter - stepSize);
				ladderAsk = Price.of(ladderCenter + stepSize);
			}
			log(INFO, "%s in %s: Randomized Ladder MID (%s, %s)", 
					this, primaryMarket, initLadderBid, initLadderAsk);
		}

		// Tick improvement
		if (this.getQuote().getBidPrice().isPresent()) {
			Price bid = getQuote().getBidPrice().get();
			ladderBid = Price.of(ladderBid.intValue() + 
					(bid.equals(ladderBid) && tickImprovement ? 
							(tickOutside ? -1 : 1) * tickSize : 0));
		}
		if (this.getQuote().getAskPrice().isPresent()) {
			Price ask = getQuote().getAskPrice().get();
			ladderAsk = Price.of(ladderAsk.intValue() +  
					(ask.equals(ladderAsk) && tickImprovement ? 
							(tickOutside ? 1 : -1) * tickSize: 0));
		}

		int ct = (numRungs-1) * stepSize;

		// min price for buy order in the ladder
		Price buyMinPrice = Price.of(ladderBid.intValue() - ct);
		// max price for buy order in the ladder
		Price buyMaxPrice = ladderBid;

		// min price for sell order in the ladder
		Price sellMinPrice = ladderAsk;
		// max price for sell order in the ladder
		Price sellMaxPrice = Price.of(ladderAsk.intValue() + ct);

		// check if the bid or ask crosses the NBBO, if truncating ladder
		if (truncateLadder) {
			BestBidAsk lastNBBOQuote = this.getNBBO();
			Price oldBuyMaxPrice = buyMaxPrice, oldSellMinPrice = sellMinPrice;
			// buy orders:  If ASK_N < Y_t, then [Y_t - C_t, ..., ASK_N]
			if (lastNBBOQuote.getBestAsk().isPresent())
				buyMaxPrice = pcomp.min(ladderBid, lastNBBOQuote.getBestAsk().get());
			// sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t]
			if (lastNBBOQuote.getBestBid().isPresent())
				sellMinPrice = pcomp.max(ladderAsk, lastNBBOQuote.getBestBid().get());
			log(INFO, "%s in %s: Truncating ladder(%s, %s)-->(%s, %s)", 
					this, primaryMarket, oldBuyMaxPrice, oldSellMinPrice, buyMaxPrice, sellMinPrice);
		}

		submitOrderLadder(buyMinPrice, buyMaxPrice, sellMinPrice, sellMaxPrice);
	}

	@Override
	protected void processTransaction(TimeStamp submitTime, OrderType type, Transaction trans) {
		super.processTransaction(submitTime, type, trans);
		postStat(Stats.MARKET_MAKER_EXECTUION_TIME + this, trans.getExecTime().getInTicks() - submitTime.getInTicks());
	}
	
	@Override
	public void liquidateAtPrice(Price price) {
		super.liquidateAtPrice(price);
		postStat(Stats.CLASS_PROFIT + "market_maker", getProfit());
	}

	@Override
	protected String name() {
		String oldName = super.name();
		return oldName.substring(0, oldName.length() - 6) + "MM";
	}

	private static final long serialVersionUID = -782740037969385370L;
	
}
