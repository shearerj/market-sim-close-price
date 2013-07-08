package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import logger.Logger;
import market.BestQuote;
import market.Bid;
import market.PQBid;
import market.PQPoint;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import data.EntityProperties;
import data.Observations;
import event.TimeStamp;

/**
 * LAAGENT
 * 
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * This agent can act infinitely fast (i.e. sleep time = 0).
 * 
 * @author ewah
 */
public class LAAgent extends HFTAgent {

	public final static String ALPHA_KEY = "alpha";

	protected final double alpha; // LA profit gap

	public LAAgent(int agentID, MarketModel model,
			int sleepTime, double sleepVar, double alpha, RandPlus rand) {
		super(agentID, Consts.START_TIME, model, sleepTime, sleepVar, rand);
		this.alpha = alpha;
	}

	public LAAgent(int agentID, MarketModel model,
			RandPlus rand, EntityProperties props) {
		this(agentID, model, props.getAsInt(SLEEPTIME_KEY, 0),
				props.getAsDouble(SLEEPVAR_KEY, 100), props.getAsDouble(
						ALPHA_KEY, 0.001), rand);
	}

	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String, Object> obs = new HashMap<String, Object>();
		obs.put(Observations.ROLE_KEY, getRole());
		obs.put(Observations.STRATEGY_KEY, getFullStrategy());
		obs.put(Observations.PAYOFF_KEY, getRealizedProfit());
		// HashMap<String,String> features = new HashMap<String,String>();
		// obs.put(Observations.FEATURES_KEY, features);
		return obs;
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp ts) {

		// Ensure that agent has arrived in the market
		if (ts.compareTo(arrivalTime) >= 0) {
			Collection<Activity> actMap = new ArrayList<Activity>();

			// update quotes
			this.updateAllQuotes(ts);
			BestQuote bestQuote = findBestBuySell();

			if ((bestQuote.bestSell > (1 + alpha) * bestQuote.bestBuy)
					&& (bestQuote.bestBuy >= 0)) {

				Logger.log(Logger.INFO, ts.toString() + " | " + this + " "
						+ agentType
						+ "::agentStrategy: Found possible arb opp!");

				int buyMarketID = bestQuote.bestBuyMarket;
				int sellMarketID = bestQuote.bestSellMarket;
				Market buyMarket = data.getMarket(buyMarketID);
				Market sellMarket = data.getMarket(sellMarketID);

				// check that BID/ASK defined for both markets
				if (buyMarket.defined() && sellMarket.defined()) {

					int midPoint = (bestQuote.bestBuy + bestQuote.bestSell) / 2;
					int buySize = getBidQuantity(bestQuote.bestBuy, midPoint
							- tickSize, buyMarketID, true);
					int sellSize = getBidQuantity(midPoint + tickSize,
							bestQuote.bestSell, sellMarketID, false);
					int quantity = Math.min(buySize, sellSize);

					if (quantity > 0 && (buyMarketID != sellMarketID)) {
						Logger.log(
								Logger.INFO,
								ts.toString()
										+ " | "
										+ this
										+ " "
										+ agentType
										+ "::agentStrategy: Exploit existing arb opp: "
										+ bestQuote
										+ " in "
										+ data.getMarket(bestQuote.bestBuyMarket)
										+ " & "
										+ data.getMarket(bestQuote.bestSellMarket));

						actMap.addAll(executeSubmitBid(buyMarket, midPoint
								- tickSize, quantity, ts));
						actMap.addAll(executeSubmitBid(sellMarket, midPoint
								+ tickSize, -quantity, ts));

					} else if (buyMarketID == sellMarketID) {
						Logger.log(
								Logger.INFO,
								ts.toString()
										+ " | "
										+ this
										+ " "
										+ agentType
										+ "::agentStrategy: No arb opp since at least 1 market does not "
										+ "have both a bid and an ask");
						// Note that this is due to a market not having both a
						// bid & ask price,
						// causing the buy and sell market IDs to be identical

					} else if (quantity == 0) {
						Logger.log(Logger.INFO, ts.toString() + " | " + this
								+ " " + agentType
								+ "::agentStrategy: No quantity available");
						// Note that if this message appears in a CDA market,
						// then the HFT
						// agent is beating the market's Clear activity, which
						// is incorrect.
					}

				} else {
					Logger.log(
							Logger.INFO,
							ts.toString()
									+ " | "
									+ this
									+ " "
									+ agentType
									+ "::agentStrategy: Market quote(s) undefined. No bid submitted.");
				}

			}
			if (sleepTime > 0) {
				TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(
						sleepTime, sleepVar)));
				actMap.add(new AgentStrategy(this, tsNew));

			} else if (sleepTime == 0) {
				// infinitely fast HFT agent
				// actMap.add(new AgentStrategy(this, Consts.INF_TIME));
			}
			return actMap;
		}
		return Collections.emptyList();
	}

	/**
	 * Get the quantity for a bid between the begin and end prices.
	 * 
	 * @param beginPrice
	 * @param endPrice
	 * @param marketID
	 * @param buy
	 *            true if buy, false if sell
	 * @return
	 */
	private int getBidQuantity(int beginPrice, int endPrice, int marketID,
			boolean buy) {

		int quantity = 0;

		for (Bid bid : data.getMarket(marketID).getBids().values()) {
			PQBid b = (PQBid) bid;

			for (PQPoint pq : b.bidTreeSet) {
				int pqPrice = pq.getPrice().getPrice();

				if (pqPrice >= beginPrice && pqPrice <= endPrice) {
					int bidQuantity = pq.getQuantity();

					// Buy
					if (buy && (bidQuantity < 0))
						quantity -= bidQuantity;
					// Sell
					if (!buy && (bidQuantity > 0))
						quantity += bidQuantity;
				}
			}
		}
		return quantity;
	}

}
