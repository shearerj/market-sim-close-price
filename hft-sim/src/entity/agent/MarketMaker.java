package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;
import logger.Log;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.InitLadderMean;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.MarketMakerReentryRate;
import systemmanager.Keys.K;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.Size;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TickOutside;
import systemmanager.Keys.Trunc;
import utils.Maths;
import utils.Rand;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.PrivateValues;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import entity.sip.BestBidAsk;
import entity.sip.MarketInfo;
import event.TimeStamp;
import event.Timeline;
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
public abstract class MarketMaker extends SMAgent {
	
	// FIXME Make these private
	protected final int stepSize;				// rung size is distance between adjacent rungs in ladder
	protected final int numRungs;				// # of ladder rungs on one side (e.g., number of buy orders)
	protected final int initLadderMean;			// for initializing ladder center
	protected final int initLadderRange; 		// for initializing ladder center
	protected final boolean truncateLadder; 	// true if truncate if NBBO crosses ladder
	protected final boolean tickImprovement;	// true if improves by a tick when mid-prices == bid/ask
	protected final boolean tickOutside;		// true if improve tick outside the quote (default inside, bid<p<ask)
	
	protected MarketMaker(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {		
		super(id, stats, timeline, log, rand, sip, fundamental, PrivateValues.zero(),
				Iterators.concat(
						Iterators.singletonIterator(TimeStamp.ZERO), // Market makers enter immediately to set an initial ladder
						Agent.exponentials(props.get(MarketMakerReentryRate.class, ReentryRate.class), rand)),
				market, props);
		
		this.numRungs = props.get(K.class);
		this.stepSize = Maths.quantize(props.get(Size.class), getTickSize());
		this.truncateLadder = props.get(Trunc.class);
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
	
	@Override
	protected void agentStrategy() {
		super.agentStrategy();
		if (getActiveOrders().isEmpty())
			postStat(Stats.MARKET_MAKER_EXEC, numRungs*2 - getActiveOrders().size());
	}

	/**
	 * Method to create activities for submitting a ladder of orders.
	 * 
	 * XXX Note that these are regular orders, not NMS orders.
	 * 
	 * buyMin < buyMax < sellMin < sellMax
	 */
	protected void submitOrderLadder(Price buyMinPrice, Price buyMaxPrice, 
			Price sellMinPrice, Price sellMaxPrice) {
		// build descending list of buy orders
		for (int p = buyMaxPrice.intValue(); p >= buyMinPrice.intValue(); p -= stepSize)
			submitOrder(BUY, Price.of(p), 1);
		// build ascending list of sell orders
		for (int p = sellMinPrice.intValue(); p <= sellMaxPrice.intValue(); p += stepSize)
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
	 * If either ladderBid or ladderAsk is null, center ladder around initLadderMean
	 * and use initLadderRange as spread (use available ladderBid/Ask if possible).
	 * 
	 * If either current bid or current ask is null, then use either lastBid or 
	 * lastAsk (or both) in lieu of the missing quote component. This will be 
	 * dealt with by truncation if lastBid/Ask will cross the current ASK/BID.
	 * 
	 * XXX MM will lose time priority if use last bid & ask, but may not be
	 * able to get around this since it doesn't know what's in the order book.
	 */
	protected void createOrderLadder(Optional<Price> initLadderBid, Optional<Price> initLadderAsk) {
		if ((!initLadderAsk.isPresent() || !initLadderBid.isPresent()) && initLadderMean == 0)
			return;
		Price ladderBid, ladderAsk;
		if (initLadderBid.isPresent()) {
			ladderBid = initLadderBid.get();
		} else {
			if (initLadderAsk.isPresent()) {
				ladderBid = Price.of(initLadderAsk.get().intValue() - initLadderRange);
			} else {
				ladderBid = Price.of(initLadderMean - initLadderRange / 2);
			}
		}
		if (initLadderAsk.isPresent()) {
			ladderAsk = initLadderAsk.get();
		} else {
			if (initLadderBid.isPresent()) {
				ladderAsk = Price.of(initLadderBid.get().intValue() + initLadderRange);
			} else {
				ladderAsk = Price.of(initLadderMean + initLadderRange / 2);
			}
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

		// XXX Consider other way of handling when quote updates with some 
		// latency, as tick improvement is only based on current market.
		// With no latency in NBBO quote, it doesn't matter, but later on this
		// may cause some issues.
		// Check if the bid or ask crosses the NBBO, if truncating ladder
		
		int numRungsTruncated = 0;
		if (truncateLadder) {
			BestBidAsk nbbo = getNBBO();
			Optional<Price> ask = nbbo.getBestAsk();
			Optional<Price> bid = nbbo.getBestBid();

			Price oldBuyMaxPrice = buyMaxPrice, oldSellMinPrice = sellMinPrice;
			
			// These are to ensure that the ladder is truncated at the original
			// rung prices, not set the center ladder price at the BID/ASK
			
			// buy orders:  If ASK_N < Y_t, then [Y_t - C_t, ..., ASK_N]
			if (ask.isPresent()) {
				int ladderBuyMax = buyMaxPrice.intValue();
				while (ladderBuyMax >= ask.get().intValue())
					ladderBuyMax -= stepSize;
				buyMaxPrice = Price.of(ladderBuyMax);
			}
			// sell orders: If BID_N > X_t, then [BID_N, ..., X_t + C_t]
			if (bid.isPresent()) {
				int ladderSellMin = sellMinPrice.intValue();
				while (ladderSellMin <= bid.get().intValue())
					ladderSellMin += stepSize;
				sellMinPrice = Price.of(ladderSellMin);
			}
			
			if (!oldBuyMaxPrice.equals(buyMaxPrice) || !oldSellMinPrice.equals(sellMinPrice)) {
				log(INFO, "%s in %s: Truncating ladder with rung size %s from (%s, %s)-->(%s, %s)", 
						this, primaryMarket, stepSize, oldBuyMaxPrice, oldSellMinPrice, buyMaxPrice, sellMinPrice);
				numRungsTruncated += !oldBuyMaxPrice.equals(buyMaxPrice) ? 
						ladderBid.intValue() - ask.get().intValue() : 0;
				numRungsTruncated += !oldSellMinPrice.equals(sellMinPrice) ? 
						bid.get().intValue() - ladderBid.intValue() : 0;
			}
		}
		// FIXME Test this statistic, because in one market maker test it produced incorrect results
		postStat(Stats.MARKET_MAKER_TRUNC, numRungsTruncated);
		
		// Tick improvement
		if (getQuote().getBidPrice().isPresent()) {
			int offset = getQuote().getBidPrice().get().equals(buyMaxPrice) && tickImprovement ? 
					(tickOutside ? -1 : 1) * getTickSize() : 0;
			buyMaxPrice = Price.of(buyMaxPrice.intValue() + offset);
			buyMinPrice = Price.of(buyMinPrice.intValue() + offset);
		}
		if (getQuote().getAskPrice().isPresent()) {
			int offset = (getQuote().getAskPrice().get().equals(sellMinPrice) && tickImprovement ? 
					(tickOutside ? 1 : -1) * getTickSize(): 0);
			sellMinPrice = Price.of(sellMinPrice.intValue() + offset);
			sellMaxPrice = Price.of(sellMaxPrice.intValue() + offset);
		}

		submitOrderLadder(buyMinPrice, buyMaxPrice, sellMinPrice, sellMaxPrice);
	}

	@Override
	protected void processTransaction(TimeStamp submitTime, OrderType type, Transaction trans) {
		super.processTransaction(submitTime, type, trans);
		postStat(Stats.MARKET_MAKER_EXECUTION_TIME + this, trans.getExecTime().getInTicks() - submitTime.getInTicks());
	}
	
	@Override
	public void liquidateAtPrice(Price price) {
		super.liquidateAtPrice(price);
		postStat(Stats.PROFIT + "market_maker", getProfit());
	}

	@Override
	protected String name() {
		String oldName = super.name();
		return oldName.endsWith("MarketMaker") ? oldName.substring(0, oldName.length() - 11) + "MM" : oldName;
	}

	private static final long serialVersionUID = -782740037969385370L;
	
}
