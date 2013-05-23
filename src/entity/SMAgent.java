package entity;

import event.*;
import market.Quote;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;


/**
 * SMAGENT
 * 
 * Single market (SM) agent, whose agent strategy is executed only within one market.
 * This does not mean that it can only trade with its specified market; it means that
 * it only checks price quotes from its primary market.
 * 
 * An SMAgent is capable of seeing the quote from its own market with zero delay.
 * It also tracks to which market it has most recently submitted a bid, as it is only
 * permitted to submit to one market at a time.
 *  
 * ORDER ROUTING (REGULATION NMS):
 * 
 * The agent's order will be routed to the alternate market ONLY if both the NBBO 
 * quote is better than the primary market's quote and the submitted bid will transact 
 * immediately given the price in the alternate market. The only difference in outcome 
 * occurs when the NBBO is out-of-date and the agent's order is routed to the main market
 * when the alternate market is actually better.
 * 
 * @author ewah
 */
public abstract class SMAgent extends Agent {

	protected Market market;
	protected Market marketSubmittedBid;		// market to which bid has been submitted
	
	
	/**
	 * Constructor for a single market agent.
	 * @param agentID
	 * @param modelID
	 * @param d
	 * @param p
	 * @param l
	 */
	public SMAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		int mktID = Integer.parseInt(params.get(SMAgent.MARKETID_KEY));
		market = data.getMarket(mktID);
	}

	/**
	 * @return market
	 */
	public Market getMarket() {
		return market;
	}
	
	/**
	 * @return main market ID for the single market agent.
	 */
	public int getMarketID() {
		return market.getID();
	}
	
	/**
	 * @return market
	 */
	public Market getMarketSubmittedBid() {
		return marketSubmittedBid;
	}
	
	/**
	 * @return main market ID for the single market agent.
	 */
	public int getMarketIDSubmittedBid() {
		return marketSubmittedBid.getID();
	}
	
	/**
	 * Returns list of alternate market IDs (i.e. excluding the main market ID).
	 * Does so by copying the list of all marketIDs in the agent's model, then removing
	 * the main market's ID.
	 * 
	 * @return 
	 */
	public ArrayList<Integer> getAltMarketIDs() {
		ArrayList<Integer> altIDs = new ArrayList<Integer>(this.getModel().getMarketIDs());
		altIDs.remove(new Integer(market.getID()));
		return altIDs;
	}
	
	/**
	 * Wrapper method to submit bid to market after checking permissions.
	 * 
	 * @param mkt
	 * @param p
	 * @param q
	 * @param duration
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitNMSBid(int p, int q, long duration, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(Consts.SUBMIT_BID_PRIORITY, 
				new SubmitNMSBid(this, p, q, duration, ts));
		return actMap;
	}

	/**
	 * Wrapper method to submit bid that never expires to market after checking permissions.
	 * 
	 * @param mkt
	 * @param p
	 * @param q
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitNMSBid(int p, int q, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(Consts.SUBMIT_BID_PRIORITY, 
				new SubmitNMSBid(this, p, q, Consts.INF_TIME, ts));
		return actMap;
	}
	
	/**
	 * Wrapper method to submit multiple-point bid to market after checking permissions.
	 * TODO - still need to finish
	 * 
	 * @param mkt
	 * @param p
	 * @param q
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitNMSMultipleBid(int[] p, int[] q, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(Consts.SUBMIT_BID_PRIORITY, 
				new SubmitNMSMultipleBid(this, p, q, ts));
		return actMap;
	}
	
	/**
	 * Agent arrives in a single market. To ensure deterministic insertion, use
	 * AgentReentry activity.
	 * 
	 * @param market
	 * @param ts
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(TimeStamp ts) {
		log.log(Log.INFO, ts.toString() + " | " + this + "->" + market.toString());
		this.enterMarket(market, ts);
		
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(Consts.ARRIVAL_PRIORITY, 
				new AgentReentry(this, Consts.BACKGROUND_ARRIVAL_PRIORITY, ts));
		// NOTE: Reentry must be inserted as priority > THRESHOLD_POST_PRIORITY
		// otherwise the infinitely fast activities will not be inserted correctly
//		actMap.insertActivity(Consts.BACKGROUND_AGENT_PRIORITY, new UpdateAllQuotes(this, ts));
//		actMap.insertActivity(Consts.BACKGROUND_AGENT_PRIORITY, new AgentStrategy(this, market, ts));
		return actMap;
	}
	
	/**
	 * Agent re-enters a market/wakes up.
	 * 
	 * @param priority
	 * @param ts
	 */
	public ActivityHashMap agentReentry(int priority, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(priority, new UpdateAllQuotes(this, ts));
		actMap.insertActivity(priority, new AgentStrategy(this, market, ts));
		return actMap;
	}
	
	/**
	 * Agent departs a specified market, if it is active. //TODO fix later
	 * 
	 * @param market
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture(TimeStamp ts) {
		market.agentIDs.remove(market.agentIDs.indexOf(this.ID));
		market.buyers.remove(market.buyers.indexOf(this.ID));
		market.sellers.remove(market.sellers.indexOf(this.ID));
		market.removeBid(this.ID, ts);
		this.exitMarket(market.ID);
		ActivityHashMap actMap = new ActivityHashMap();
		return actMap;
	}

	/**
	 * Submit a bid to one of the possible markets, as following the National Market
	 * System (NMS) regulations. The market selected will be that with the best available
	 * price, according the NBBO.
	 * 
	 * Bid submitted never expires.
	 * 
	 * @param p
	 * @param q
	 * @param ts
	 * @return
	 */
	public ActivityHashMap executeSubmitNMSBid(int p, int q, TimeStamp ts) {
		return executeSubmitNMSBid(p, q, Consts.INF_TIME, ts);
	}
	
	/**
	 * Submit a bid to one of the possible markets, as following the National Market
	 * System (NMS) regulations. The market selected will be that with the best available
	 * price, according the NBBO.
	 * 
	 * @param p
	 * @param q
	 * @param duration
	 * @param ts
	 * @return
	 */
	public ActivityHashMap executeSubmitNMSBid(int p, int q, long duration, TimeStamp ts) {
		
		ActivityHashMap actMap = new ActivityHashMap();
		
		ArrayList<Integer> altMarketIDs = getAltMarketIDs();
		int altMarketID = market.getID();	// initialize as main market ID
		
		// TODO - enable for more than two markets total (including main)
		if (altMarketIDs.size() > 1) {
			System.err.println("Agent::executeSubmitNMSBid: 2 markets permitted currently.");
			System.exit(1);
		} else if (altMarketIDs.size() == 1) {
			// get first alternate market since there are two markets total
			altMarketID = altMarketIDs.get(0);
		}
		
		// Set additional string to log bid's duration, if it expires
		String logDuration = "";
		if (duration != Consts.INF_TIME && duration > 0) {
			logDuration = ", duration=" + duration;
		}
		
		// Identify best market, as based on the NBBO.
		Quote mainMarketQuote = market.quote(ts);
		
		// Check if NBBO indicates that other market better:
		// - Want to buy for as low a price as possible, so find market with the lowest ask.
		// - Want to sell for as high a price as possible, so find market with the highest bid.
		// - NBBO also better if the bid or ask in the current does not exist while NBBO does exist.
		boolean nbboBetter = false;
		if (q > 0) {
			if (lastNBBOQuote.bestAsk < mainMarketQuote.lastAskPrice.getPrice() &&
					lastNBBOQuote.bestAsk != -1 ||
					mainMarketQuote.lastAskPrice.getPrice() == -1 &&
					lastNBBOQuote.bestAsk != -1) { 
				nbboBetter = true;
			}
		} else {
			if (lastNBBOQuote.bestBid > mainMarketQuote.lastBidPrice.getPrice() ||
					mainMarketQuote.lastBidPrice.getPrice() == -1) {
				// don't need lastNBBOQuote.bestBid != -1 due to first condition, will always > -1
				nbboBetter = true;
			}
		}

		int bestMarketID = market.getID();
		if (nbboBetter) {
			// nbboBetter = true indicates that the alternative market has a better quote
			log.log(Log.INFO, ts + " | " + this + " " + agentType + 
					"::submitNMSBid: " + "NBBO(" + lastNBBOQuote.bestBid + ", " + 
					lastNBBOQuote.bestAsk + ") better than " + market + 
					" Quote(" + mainMarketQuote.lastBidPrice.getPrice() + 
					", " + mainMarketQuote.lastAskPrice.getPrice() + ")");
			
			int bestPrice = -1;
			if (q > 0) {
				// Ensure that NBBO ask is defined, otherwise submit to current market
				if (p >= lastNBBOQuote.bestAsk && lastNBBOQuote.bestAsk != -1) {
					bestMarketID = altMarketID;
					bestPrice = lastNBBOQuote.bestAsk;
				}
			} else {
				if (p <= lastNBBOQuote.bestBid) {
					bestMarketID = altMarketID;
					bestPrice = lastNBBOQuote.bestBid;
				}
			}
			
			if (bestMarketID == altMarketID) {				
				log.log(Log.INFO, ts + " | " + this + " " + agentType + 
						"::submitNMSBid: " + "Bid +(" + p + "," + q + ") will transact" +
						" immediately in " + data.getMarket(altMarketID) +
						" given best price " + bestPrice);
			}
			
			// submit bid to the best market
			marketSubmittedBid = data.getMarket(bestMarketID);
			actMap.appendActivityHashMap(submitBid(marketSubmittedBid, p, q, ts));
			log.log(Log.INFO, ts + " | " + this + " " + agentType + 
					"::submitNMSBid: " + "+(" + p + "," + q + ") to " + 
					marketSubmittedBid + logDuration);
			
		} else {
			// main market is better than the alternate market (according to NBBO)
			log.log(Log.INFO, ts + " | " + this + " " + agentType + 
					"::submitNMSBid: " + "NBBO(" + lastNBBOQuote.bestBid + ", " + 
					lastNBBOQuote.bestAsk + ") worse than/same as " + market + 
					" Quote(" + mainMarketQuote.lastBidPrice.getPrice() + 
					", " + mainMarketQuote.lastAskPrice.getPrice() + ")");
			
			// submit bid to the main market
			marketSubmittedBid = market;
			actMap.appendActivityHashMap(submitBid(market, p, q, ts));
			log.log(Log.INFO, ts + " | " + this + " " + agentType + 
					"::submitNMSBid: " + "+(" + p + "," + q + ") to " + 
					market + logDuration);
		}
		
		if (duration != Consts.INF_TIME && duration > 0) {
			// Bid expires after a given duration
			actMap.appendActivityHashMap(expireBid(marketSubmittedBid, duration, ts));
		}
		return actMap;
	}
	
	/**
	 * Submits a multiple-point/offer bid to one of the possible markets, as following 
	 * the National Market System (NMS) regulations. The market selected will be that 
	 * with the best available price, according the NBBO.
	 * 
	 * TODO - postponed...
	 * 
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap executeSubmitNMSMultipleBid(int[] price, int[] quantity, TimeStamp ts) {
		
		ActivityHashMap actMap = new ActivityHashMap();
		ArrayList<Integer> altMarketIDs = getAltMarketIDs();
//		int altMarketID = market.getID();	// initialize as main market ID

		// TODO - enable for more than two markets total (including main)
		if (altMarketIDs.size() > 1) {
			System.err.println("Agent::executeSubmitNMSBid: 2 markets permitted currently.");
		} else if (altMarketIDs.size() == 1) {
			// get first alternate market since there are two markets total
//			altMarketID = altMarketIDs.get(0);
		}
		
		return actMap;
	}

	
}
