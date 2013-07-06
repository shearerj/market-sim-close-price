package entity;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import market.BestBidAsk;
import market.Price;
import market.PrivateValue;
import market.Quote;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitNMSBid;
import activity.SubmitNMSMultipleBid;
import event.TimeStamp;

/**
 * SMAGENT
 * 
 * Single market (SM) agent, whose agent strategy is executed only within one
 * market. This does not mean that it can only trade with its specified market;
 * it means that it only checks price quotes from its primary market.
 * 
 * An SMAgent is capable of seeing the quote from its own market with zero
 * delay. It also tracks to which market it has most recently submitted a bid,
 * as it is only permitted to submit to one market at a time.
 * 
 * ORDER ROUTING (REGULATION NMS):
 * 
 * The agent's order will be routed to the alternate market ONLY if both the
 * NBBO quote is better than the primary market's quote and the submitted bid
 * will transact immediately given the price in the alternate market. The only
 * difference in outcome occurs when the NBBO is out-of-date and the agent's
 * order is routed to the main market when the alternate market is actually
 * better.
 * 
 * @author ewah
 */
public abstract class SMAgent extends Agent {

	protected final Market market;
	// market to which bid has been submitted
	protected Market marketSubmittedBid;

	public SMAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, PrivateValue pv, RandPlus rand) {
		super(agentID, arrivalTime, model, pv, rand);
		this.market = market;
	}

	/**
	 * @return market
	 */
	public Market getMarket() {
		return market;
	}

	/**
	 * @return market
	 */
	public Market getMarketSubmittedBid() {
		return marketSubmittedBid;
	}

	/**
	 * Wrapper method to submit bid to market after checking permissions.
	 */
	public Collection<? extends Activity> submitNMSBid(Price price,
			int quantity, TimeStamp duration, TimeStamp ts) {
		return Collections.singleton(new SubmitNMSBid(this, price, quantity,
				duration, ts));
	}

	/**
	 * Wrapper method to submit multiple-point bid to market after checking
	 * permissions. TODO - still need to finish
	 */
	public Collection<? extends Activity> submitNMSMultipleBid(List<Integer> p,
			List<Integer> q, TimeStamp ts) {
		return Collections.singleton(new SubmitNMSMultipleBid(this, p, q, ts));
	}

	/**
	 * Agent arrives in a single market.
	 * 
	 * @param market
	 * @param ts
	 * @return Collection<Activity>
	 */
	public Collection<? extends Activity> agentArrival(TimeStamp ts) {
		log(INFO,
				ts.toString() + " | " + this + "->" + market.toString());
		this.enterMarket(market, ts);
		// FIXME I think with the new event queue, this should be instantaneous
		// not at the same time
		return Collections.singleton(new AgentStrategy(this, market, Consts.INF_TIME));
	}

	/**
	 * Agent departs a specified market, if it is active. //TODO fix later
	 * 
	 * @param market
	 * @return Collection<Activity>
	 */
	public Collection<Activity> agentDeparture(TimeStamp ts) {
		market.agentIDs.remove(market.agentIDs.indexOf(this.id));
		market.buyers.remove(market.buyers.indexOf(this.id));
		market.sellers.remove(market.sellers.indexOf(this.id));
		market.removeBid(this.id, ts);
		this.exitMarket(market.id);
		return Collections.emptyList();
	}

	/**
	 * Updates quote for agent's primary market.
	 * 
	 * @param ts
	 * @return
	 */
	@Deprecated
	// I don't think this needs so / should be called anymore. The agent should
	// just update information at strategy time and go from there
	public Collection<? extends Activity> updateAllQuotes(TimeStamp ts) {
		updateQuotes(market, ts);
		return this.executeUpdateAllQuotes(ts);
	}

	/**
	 * Submit a bid to one of the possible markets, as following the National
	 * Market System (NMS) regulations. The market selected will be that with
	 * the best available price, according the NBBO.
	 * 
	 * Bid submitted never expires.
	 * 
	 * @param p
	 * @param q
	 * @param ts
	 * @return
	 */
	@Deprecated
	public Collection<? extends Activity> executeSubmitNMSBid(int p, int q,
			TimeStamp ts) {
		return executeSubmitNMSBid(new Price(p), q, ts);
	}

	/**
	 * Submit a bid to one of the possible markets, as following the National
	 * Market System (NMS) regulations. The market selected will be that with
	 * the best available price, according the NBBO.
	 * 
	 * @param price
	 * @param quantity
	 * @param duration
	 * @param ts
	 * @return
	 */
	@Deprecated
	public Collection<? extends Activity> executeSubmitNMSBid(int price,
			int quantity, TimeStamp duration, TimeStamp ts) {
		return executeSubmitNMSBid(new Price(price), quantity, duration, ts);
	}

	/**
	 * Submit a bid to one of the possible markets, as following the National
	 * Market System (NMS) regulations. The market selected will be that with
	 * the best available price, according the NBBO.
	 */
	public Collection<? extends Activity> executeSubmitNMSBid(Price price,
			int quantity, TimeStamp duration, TimeStamp ts) {
		if (quantity == 0)
			return Collections.emptySet();

		BestBidAsk lastNBBOQuote = sip.getNBBOQuote(model.getID());

		// Identify best market, as based on the NBBO.
		Quote mainMarketQuote = market.quote(ts);

		// Check if NBBO indicates that other market better:
		// - Want to buy for as low a price as possible, so find market with the
		// lowest ask.
		// - Want to sell for as high a price as possible, so find market with
		// the highest bid.
		// - NBBO also better if the bid or ask in the current does not exist
		// while NBBO does exist.

		boolean nbboBetter, willTransact;
		Market bestMarket;
		Price bestPrice;

		if (quantity > 0) { // buy
			nbboBetter = lastNBBOQuote.getBestAsk() != null
					&& lastNBBOQuote.getBestAsk().lessThan(mainMarketQuote.lastAskPrice);
			willTransact = nbboBetter
					&& price.greaterThan(lastNBBOQuote.getBestAsk());
			if (willTransact) {
				bestMarket = lastNBBOQuote.getBestAskMarket();
				bestPrice = lastNBBOQuote.getBestAsk();
			} else {
				bestMarket = market;
				bestPrice = mainMarketQuote.lastAskPrice;
			}
		} else { // sell
			nbboBetter = lastNBBOQuote.getBestBid() != null
					&& lastNBBOQuote.getBestBid().greaterThan(mainMarketQuote.lastBidPrice);
			willTransact = nbboBetter && price.lessThan(lastNBBOQuote.getBestBid());
			if (willTransact) {
				bestMarket = lastNBBOQuote.getBestBidMarket();
				bestPrice = lastNBBOQuote.getBestBid();
			} else {
				bestMarket = market;
				bestPrice = mainMarketQuote.lastBidPrice;
			}
		}

		if (nbboBetter)
			log(INFO, ts + " | " + this + " " + agentType
					+ "::submitNMSBid: " + "NBBO(" + lastNBBOQuote.getBestBid()
					+ ", " + lastNBBOQuote.getBestAsk() + ") better than " + market
					+ " Quote(" + mainMarketQuote.lastBidPrice + ", "
					+ mainMarketQuote.lastAskPrice + ")");
		if (willTransact)
			log(INFO, ts + " | " + this + " " + agentType
					+ "::submitNMSBid: " + "Bid +(" + price + "," + quantity
					+ ") will transact" + " immediately in " + bestMarket
					+ " given best price " + bestPrice);

		// submit bid to the best market
		marketSubmittedBid = bestMarket;
		Collection<Activity> actMap = new ArrayList<Activity>(executeSubmitBid(
				bestMarket, price, quantity, ts));

		String durationLog = duration != Consts.INF_TIME
				&& duration.longValue() > 0 ? ", duration=" + duration : "";
		log(INFO, ts + " | " + this + " " + agentType
				+ "::submitNMSBid: " + "+(" + price + "," + quantity + ") to "
				+ bestMarket + durationLog);

		if (duration != Consts.INF_TIME && duration.longValue() > 0) {
			// Bid expires after a given duration
			actMap.addAll(expireBid(bestMarket, duration, ts));
		}
		return actMap;
	}

	public Collection<? extends Activity> executeSubmitNMSBid(Price p, int q,
			TimeStamp ts) {
		return executeSubmitNMSBid(p, q, Consts.INF_TIME, ts);
	}

	/**
	 * Submits a multiple-point/offer bid to one of the possible markets, as
	 * following the National Market System (NMS) regulations. The market
	 * selected will be that with the best available price, according the NBBO.
	 * 
	 * TODO - postponed...
	 * 
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public Collection<Activity> executeSubmitNMSMultipleBid(
			List<Integer> price, List<Integer> quantity, TimeStamp ts) {

		Collection<Activity> actMap = new ArrayList<Activity>();
		// ArrayList<Integer> altMarketIDs = getAltMarketIDs();
		// int altMarketID = market.getID(); // initialize as main market ID

		// TODO - enable for more than two markets total (including main)
		// if (altMarketIDs.size() > 1) {
		// System.err.println(this.getClass().getSimpleName()
		// + "::executeSubmitNMSBid: 2 markets permitted currently.");
		// } else if (altMarketIDs.size() == 1) {
		// // get first alternate market since there are two markets total
		// // altMarketID = altMarketIDs.get(0);
		// }

		return actMap;
	}

}
