package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import logger.Logger;
import model.MarketModel;
import utils.MathUtils;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import activity.UpdateAllQuotes;
import data.EntityProperties;
import data.Observations;
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

	protected final int stepSize;
	protected final int rungSize;
	protected final int numRungs; // # of ladder rungs on one side (e.g., number
									// of buy orders)
	protected int xt, yt; // stores the ask/bid, respectively FIXME Prices
							// instead, null instead of -1, ask / bid instead of
							// xt, yt, just gather during strategy usntead of
							// updating?
	protected final TimeStamp sleepTime;

	public BasicMarketMaker(int agentID, MarketModel model, Market market,
			TimeStamp sleepTime, int numRungs, int rungSize, RandPlus rand) {
		super(agentID, model, market, rand);
		this.sleepTime = sleepTime;
		this.numRungs = numRungs;
		this.rungSize = rungSize;
		this.stepSize = MathUtils.quantize(rungSize, data.tickSize);

		this.xt = -1; // ask
		this.yt = -1; // bid
	}

	public BasicMarketMaker(int agentID, MarketModel model, Market market,
			RandPlus rand, EntityProperties params) {
		this(agentID, model, market, new TimeStamp(
				params.getAsLong(SLEEPTIME_KEY, 200)),
				params.getAsInt(NUMRUNGS_KEY, 10), params.getAsInt(RUNGSIZE_KEY, 1000),
				rand);
		// SLEEPTIME_VAR = 100
	}

	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String, Object> obs = new HashMap<String, Object>();
		obs.put(Observations.ROLE_KEY, getRole());
		obs.put(Observations.PAYOFF_KEY, getRealizedProfit());
		obs.put(Observations.STRATEGY_KEY, getFullStrategy());
		return obs;
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();

		// update all quotes
		this.updateQuotes(market, ts);

		int bid = getBidPrice(market.getID()).getPrice();
		int ask = getAskPrice(market.getID()).getPrice();

		// check that bid or ask is defined
		if (bid <= 0 || ask <= 0) {
			Logger.log(Logger.INFO, ts + " | " + this + " " + agentType
					+ "::agentStrategy: undefined quote in market "
					+ getMarket());

		} else {
			// check if bid/ask has changed; if so, submit fresh orders
			if (bid != yt || ask != xt) {
				ArrayList<Integer> prices = new ArrayList<Integer>();
				ArrayList<Integer> quantities = new ArrayList<Integer>();

				int ct = numRungs * stepSize;
				int buyMinPrice = bid - ct; // min price for buy order in the
											// ladder
				int sellMaxPrice = ask + ct; // max price for sell order in the
												// ladder

				// check if the bid or ask crosses the NBBO
				if (lastNBBOQuote.bestAsk.getPrice() < ask) {
					// buy orders: If ASK_N < X_t, then [ASK_N, ..., Y_t]
					buyMinPrice = lastNBBOQuote.bestAsk.getPrice();
				}
				if (lastNBBOQuote.bestBid.getPrice() > bid) {
					// sell orders: If BID_N > Y_t, then [X_t, ..., BID_N]
					sellMaxPrice = lastNBBOQuote.bestBid.getPrice();
				}

				// submits only one side if either bid or ask is undefined
				if (bid > 0) {
					// build descending list of buy orders (yt, ..., yt - ct) or
					// stops at NBBO ask
					for (int p = bid; p >= buyMinPrice; p -= stepSize) {
						if (p > 0) {
							prices.add(p);
							quantities.add(1);
						}
					}
				}
				if (ask > 0) {
					// build ascending list of sell orders (xt, ..., xt + ct) or
					// stops at NBBO bid
					for (int p = ask; p <= sellMaxPrice; p += stepSize) {
						prices.add(p);
						quantities.add(-1);
					}
				}

				Logger.log(Logger.INFO, ts + " | " + getMarket() + " " + this
						+ " " + agentType + "::agentStrategy: ladder numRungs="
						+ numRungs + ", stepSize=" + stepSize + ": buys ["
						+ buyMinPrice + ", " + bid + "] &" + " sells [" + ask
						+ ", " + sellMaxPrice + "]");
				actMap.addAll(submitMultipleBid(getMarket(), prices,
						quantities, ts));
			} else {
				Logger.log(Logger.INFO, ts + " | " + getMarket() + " " + this
						+ " " + agentType
						+ "::agentStrategy: no change in submitted ladder.");
			}
		}
		// update latest bid/ask prices
		xt = ask;
		yt = bid;

		// insert activities for next time the agent wakes up
		// TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime,
		// sleepVar)));
		TimeStamp tsNew = ts.sum(new TimeStamp(sleepTime));
		actMap.add(new UpdateAllQuotes(this, tsNew));
		actMap.add(new AgentStrategy(this, market, tsNew));
		return actMap;
	}

}
