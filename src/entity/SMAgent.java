package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Iterator;

import market.Quote;

/**
 * Single market (SM) agent, whose agent strategy is executed only within one market.
 * This does not mean that it can only trade with its specified market; however, it is
 * only capable of looking at price quotes from the NBBO and its market.
 * 
 * An SMAgent is only capable of seeing the quote from its own market with zero delay.
 * It also tracks to which market it has most recently submitted a bid, as it is only
 * permitted to submit to one market at a time.
 *  
 * BID SUBMISSION:
 * 
 * The agent will submit to the alternate market ONLY if both the NBBO quote is better
 * than the main market's quote and the bid to submit will transact immediately 
 * given the price in the alternate market. The only difference in outcome occurs
 * when the NBBO is out-of-date and the agent submits a bid to the main market
 * although the alternate market is actually better.
 * 
 * @author ewah
 */
public abstract class SMAgent extends Agent {

	protected Market market;
	protected Market marketSubmittedBid;		// market to which its bid has been submitted

	/**
	 * Constructor for a single market agent.
	 * @param agentID
	 * @param modelID
	 * @param d
	 * @param p
	 * @param l
	 * @param mktID		sets the main market for the SMAgent
	 */
	public SMAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l, int mktID) {
		super(agentID, modelID, d, p, l);
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
	public int getMarketSubmittedBidID() {
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
	 * @param price
	 * @param quantity
	 * @param duration
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitNMSBid(int price, int quantity, long duration, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		if (data.getModelByMarketID(market.getID()).checkAgentPermissions(this.ID)) {
			actMap.insertActivity(Consts.SUBMIT_BID_PRIORITY, 
					new SubmitNMSBid(this, price, quantity, duration, ts));
		}
		return actMap;
	}

	/**
	 * Wrapper method to submit multiple-point bid to market after checking permissions.
	 * TODO - still need to finish
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	public ActivityHashMap submitNMSMultipleBid(int[] price, int[] quantity, TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();
		if (data.getModelByMarketID(market.getID()).checkAgentPermissions(this.ID)) {
			actMap.insertActivity(Consts.SUBMIT_BID_PRIORITY, 
					new SubmitNMSMultipleBid(this, price, quantity, ts));
		}
		return actMap;
	}
	
	/**
	 * Agent arrives in a single market.
	 * 
	 * @param market
	 * @param ts
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentArrival(TimeStamp ts) {
		log.log(Log.INFO, ts.toString() + " | " + this + "->" + market.toString());
		this.enterMarket(market, ts);
		
		ActivityHashMap actMap = new ActivityHashMap();
		actMap.insertActivity(new UpdateAllQuotes(this, ts));
		actMap.insertActivity(new AgentStrategy(this, market, ts));
		return actMap;
	}
	
	/**
	 * Agent departs a specified market, if it is active.
	 * 
	 * @param market
	 * @return ActivityHashMap
	 */
	public ActivityHashMap agentDeparture() {
		
		market.agentIDs.remove(market.agentIDs.indexOf(this.ID));
		market.buyers.remove(market.buyers.indexOf(this.ID));
		market.sellers.remove(market.sellers.indexOf(this.ID));
		market.removeBid(this.ID, null);
		this.exitMarket(market.ID);
		ActivityHashMap actMap = new ActivityHashMap();
		return actMap;
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
		} else if (altMarketIDs.size() == 1) {
			// get first alternate market since there are two markets total
			altMarketID = altMarketIDs.get(0);
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
					marketSubmittedBid + ", duration=" + duration);
			
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
					market + ", duration=" + duration);
			
		}
		
		// Bid expires after a given duration
		actMap.appendActivityHashMap(expireBid(marketSubmittedBid, duration, ts));
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
		int altMarketID = market.getID();	// initialize as main market ID

		// TODO - enable for more than two markets total (including main)
		if (altMarketIDs.size() > 1) {
			System.err.println("Agent::executeSubmitNMSBid: 2 markets permitted currently.");
		} else if (altMarketIDs.size() == 1) {
			// get first alternate market since there are two markets total
			altMarketID = altMarketIDs.get(0);
		}
		
		return actMap;
	}

	
}
