package entity;


import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import market.BestQuote;
import market.Bid;
import market.PQBid;
import market.PQPoint;
import market.Price;
import market.PrivateValue;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import data.EntityProperties;
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

	protected final double alpha; // LA profit gap FIXME Different from private
									// value
	
	protected Collection<LAInformationProcessor> ip_las;
	private TimeStamp latency;

	public LAAgent(int agentID, MarketModel model, int sleepTime,
			double sleepVar, double alpha, RandPlus rand) {
		super(agentID, Consts.START_TIME, model, sleepTime, sleepVar, rand);
		this.alpha = alpha;
	}

	public LAAgent(int agentID, MarketModel model, RandPlus rand,
			EntityProperties props, TimeStamp latency) {
		this(agentID, model, props.getAsInt(SLEEPTIME_KEY, 0),
				props.getAsDouble(SLEEPVAR_KEY, 100), props.getAsDouble(
						ALPHA_KEY, 0.001), rand);
		this.latency = latency;
		setupIP();
	}
	
	private void setupIP() {
		for (Market market : model.getMarkets()) {
			this.addIP(new LAInformationProcessor(model.getipIDgen(), market.getID(), latency, market, this), market);
		}
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp ts) {

		// Ensure that agent has arrived in the market
		if (ts.compareTo(arrivalTime) >= 0) {
			Collection<Activity> actMap = new ArrayList<Activity>();

			// update quotes
			BestQuote bestQuote = findBestBuySell();

			if ((bestQuote.getBestSell().getPrice() > (1 + alpha)
					* bestQuote.getBestBuy().getPrice())
					&& (bestQuote.getBestBuy().getPrice() >= 0)) {

				log(INFO, ts.toString() + " | " + this + " " + agentType
						+ "::agentStrategy: Found possible arb opp!");

				Market buyMarket = bestQuote.getBestBuyMarket();
				Market sellMarket = bestQuote.getBestSellMarket();

				// check that BID/ASK defined for both markets
				if (buyMarket.defined() && sellMarket.defined()) {

					int midPoint = (bestQuote.getBestBuy().getPrice() + bestQuote.getBestSell().getPrice()) / 2;
					int buySize = getBidQuantity(bestQuote.getBestBuy(),
							new Price(midPoint - tickSize), buyMarket, true);
					int sellSize = getBidQuantity(
							new Price(midPoint + tickSize),
							bestQuote.getBestSell(), sellMarket, false);
					int quantity = Math.min(buySize, sellSize);

					if (quantity > 0 && !(buyMarket.equals(sellMarket))) {
						log(INFO,
								ts.toString()
										+ " | "
										+ this
										+ " "
										+ agentType
										+ "::agentStrategy: Exploit existing arb opp: "
										+ bestQuote + " in "
										+ bestQuote.getBestBuyMarket() + " & "
										+ bestQuote.getBestSellMarket());

						// XXX Midpoint isn't quantized, so they'll be ticksize
						// apart, but not actuall on a ticksize...
						actMap.addAll(executeSubmitBid(buyMarket, new Price(
								midPoint - tickSize), quantity, ts));
						actMap.addAll(executeSubmitBid(sellMarket, new Price(
								midPoint + tickSize), -quantity, ts));

					} else if (buyMarket.equals(sellMarket)) {
						log(INFO,
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
						log(INFO, ts.toString() + " | " + this + " "
								+ agentType
								+ "::agentStrategy: No quantity available");
						// Note that if this message appears in a CDA market,
						// then the HFT
						// agent is beating the market's Clear activity, which
						// is incorrect.
					}

				} else {
					log(INFO,
							ts.toString()
									+ " | "
									+ this
									+ " "
									+ agentType
									+ "::agentStrategy: Market quote(s) undefined. No bid submitted.");
				}

			}
			if (sleepTime > 0) {
				TimeStamp tsNew = ts.sum(new TimeStamp(
						(long) rand.nextGaussian(sleepTime, sleepVar)));
				actMap.add(new AgentStrategy(this, tsNew));

			} else if (sleepTime == 0) {
				// infinitely fast HFT agent
				// actMap.add(new AgentStrategy(this, Consts.INF_TIME));
			}
			return actMap;
		}
		return Collections.emptyList();
	}
	
	public void addIP(LAInformationProcessor laip, Market market) {
		this.ip_las.add(laip);
		market.addIP(laip);
	}

	/**
	 * Find best market to buy in (i.e. lowest ask) and to sell in (i.e. highest
	 * bid). This is a global operation so it checks all markets in marketIDs
	 * and it gets the up-to-date market quote with zero delays.
	 * 
	 * bestBuy = the best price an agent can buy at (the lowest sell bid).
	 * bestSell = the best price an agent can sell at (the highest buy bid).
	 * 
	 * NOTE: This uses only those markets belonging to the agent's model, as
	 * strategies can only be selected based on information on those markets.
	 * 
	 * TODO eventually this should use InformationProcessors
	 * 
	 * @return BestQuote
	 */
	protected BestQuote findBestBuySell() {
		Price bestBuy = null, bestSell = null;
		Market bestBuyMkt = null, bestSellMkt = null;

		for (LAInformationProcessor laip : ip_las) {
			// TODO This should use IP not modle.markets
			Price bid = laip.getNBBOQuote(1).getBestBid(); // TODO shouldn't have # as input
			Price ask = laip.getNBBOQuote(1).getBestAsk();

			// in case the bid/ask disappears
			ArrayList<Price> price = new ArrayList<Price>();
			price.add(bid);
			price.add(ask);

			// Best market to buy in is the one with the lowest ASK
			if (ask.lessThan(bestBuy)) {
				bestBuy = ask;
				bestBuyMkt = laip.getMarket();
			}
			// Best market to sell in is the one with the highest BID
			if (bid.greaterThan(bestSell)) {
				bestSell = bid;
				bestSellMkt = laip.getMarket();
			}
		}
		return new BestQuote(bestBuyMkt, bestBuy, bestSellMkt, bestSell);
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
	protected int getBidQuantity(Price beginPrice, Price endPrice,
			Market marketID, boolean buy) {

		int quantity = 0;

		for (Bid bid : marketID.getBids().values()) {
			PQBid b = (PQBid) bid;

			for (PQPoint pq : b.bidTreeSet) {
				Price pqPrice = pq.getPrice();

				if (pqPrice.greaterThanEquals(beginPrice)
						&& pqPrice.lessThanEqual(endPrice)) {
					int bidQuantity = pq.getQuantity();

					// FIXME this seems like it might return the wrong thing
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
