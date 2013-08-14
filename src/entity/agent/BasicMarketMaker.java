package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import static utils.Compare.max;
import static utils.Compare.min;

import java.util.ArrayList;
import java.util.Collection;

import model.MarketModel;
import systemmanager.Keys;
import utils.MathUtils;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitOrder;
import activity.WithdrawOrder;
import data.EntityProperties;
import entity.infoproc.BestBidAsk;
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
 * based on BID = Y_t < ASK = X_t, where C_t = numRungs * stepSize:
 * 
 * buy orders: [Y_t - C_t, ..., Y_t - 1, Y_t] sell orders: [X_t, X_t + 1, ...,
 * X_t + C_t]
 * 
 * The market maker liquidates its position at the price dictated by the global
 * fundamental at the end of the simulation (event is inserted in SystemSetup).
 * 
 * NOTE: The MarketMakerAgent will truncate the ladder when the price crosses
 * the NBBO, i.e., whenever one of the points in the bid would be routed to the
 * alternate market otherwise. This happens when:
 * 
 * buy orders: If ASK_N < X_t, then [ASK_N, ..., Y_t] (ascending) sell orders:
 * If BID_N > Y_t, then [X_t, ..., BID_N] (ascending)
 * 
 * @author ewah, gshiva
 */
public class BasicMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 9057600979711100221L;
	
	protected final int stepSize;
	protected final int rungSize;
	// # of ladder rungs on one side (e.g., number of buy orders)
	protected final int numRungs;
	protected Price lastAsk, lastBid; // stores the last ask/bid, respectively
	protected final TimeStamp sleepTime;

	public BasicMarketMaker(MarketModel model, Market market,
			TimeStamp sleepTime, int numRungs, int rungSize, RandPlus rand,
			int tickSize) {
		super(model, market, rand, tickSize);
		this.sleepTime = sleepTime;
		this.numRungs = numRungs;
		this.rungSize = rungSize;
		this.stepSize = MathUtils.quantize(rungSize, tickSize);
		this.lastAsk = null;
		this.lastBid = null;
	}

	public BasicMarketMaker(MarketModel model, Market market,
			RandPlus rand, EntityProperties params) {
		this(model, market, new TimeStamp(params.getAsLong(
				Keys.SLEEP_TIME, 200)), params.getAsInt(Keys.NUM_RUNGS, 10),
				params.getAsInt(Keys.RUNG_SIZE, 1000), rand, params.getAsInt(
						Keys.TICK_SIZE, 1));
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>();

		// update NBBO
		BestBidAsk lastNBBOQuote = sip.getNBBO();

		Quote quote = marketIP.getQuote();
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
				
				Price ct = new Price(numRungs * stepSize);
				// min price for buy order in the ladder
				Price buyMinPrice = min(bid.minus(ct), lastNBBOQuote.getBestAsk());
				// max price for sell order in the ladder
				Price sellMaxPrice = max(ask.plus(ct), lastNBBOQuote.getBestBid());

				// check if the bid or ask crosses the NBBO
				// FIXME I believe this will create orders that would transact on
				// another market. Wait to hear from Elaine.
				if (lastNBBOQuote.getBestAsk().lessThan(ask)) {
					// buy orders: If ASK_N < X_t, then [ASK_N, ..., Y_t]
					buyMinPrice = lastNBBOQuote.getBestAsk();
				}
				if (lastNBBOQuote.getBestBid().greaterThan(bid)) {
					// sell orders: If BID_N > Y_t, then [X_t, ..., BID_N]
					sellMaxPrice = lastNBBOQuote.getBestBid();
				}

				// TODO change from this implementation, which only works if both
				// sides are defined, to one that looks at each side individually.

				// build descending list of buy orders (yt, ..., yt - ct) or
				// stops at NBBO ask
				for (int price = bid.getInTicks(); price >= buyMinPrice.getInTicks(); price -= stepSize)
					acts.add(new SubmitOrder(this, primaryMarket, new Price(price), 1, TimeStamp.IMMEDIATE));

				// build ascending list of sell orders (xt, ..., xt + ct) or
				// stops at NBBO bid
				for (int price = ask.getInTicks(); price <= sellMaxPrice.getInTicks(); price += stepSize)
					acts.add(new SubmitOrder(this, primaryMarket, new Price(price), -1, TimeStamp.IMMEDIATE));

				log(INFO, primaryMarket + " " + this + " " + getName()
						+ "::agentStrategy: ladder numRungs=" + numRungs
						+ ", stepSize=" + stepSize + ": buys [" + buyMinPrice
						+ ", " + bid + "] &" + " sells [" + ask + ", "
						+ sellMaxPrice + "]");
				
			}
		} else {
			log(INFO, currentTime + " | " + primaryMarket + " " + this + " " + getName()
					+ "::agentStrategy: no change in submitted ladder.");
		}
		// update latest bid/ask prices
		lastAsk = ask;
		lastBid = bid;

		// insert activities for next time the agent wakes up
		// TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime,
		// sleepVar)));
		TimeStamp tsNew = currentTime.plus(sleepTime);
		acts.add(new AgentStrategy(this, tsNew));
		return acts;
	}

}
