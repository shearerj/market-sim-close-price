package market;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import utils.MathUtils;
import entity.Agent;
import entity.Market;
import event.TimeStamp;

/**
 * Price-quantity order book for a market.
 * 
 * @author ewah
 */
public class PQOrderBook extends OrderBook {

	public FourHeap FH;
	
	public PQOrderBook(Market market) {
		super(market);
		FH = new FourHeap(market);
	}

	/** 
	 * inserts new bid into the order book
	 */
	public void insertBid(Bid newBid) {
		Agent agent = newBid.getAgent();

		// create entry if non-existent, remove old bid if exists
		if (!activeBids.containsKey(agent)) {
			activeBids.put(agent, (PQBid) newBid);
		} else {
			PQBid oldBid = (PQBid) activeBids.get(agent);
			for (Iterator<PQPoint> i = oldBid.bidTreeSet.iterator(); i.hasNext(); ) {
				FH.removeBid(i.next());
			}
			activeBids.put(agent, (PQBid) newBid);
		}
		PQBid pqBid = (PQBid) newBid;
		//insert all new Points from this Bid into the 4Heap
		for (Iterator<PQPoint> i = pqBid.bidTreeSet.iterator(); i.hasNext(); ) {
			PQPoint p = i.next();
			p.Parent = pqBid;
			FH.insertBid(p);
		}
	}

	/**
	 * inserts a pq point into the order book, but doesn't update FH.
	 * For updating activeBids list in clear method.
	 * @param newPoint
	 */
	public void insertPoint(Point newPoint) {
		PQPoint pq = (PQPoint) newPoint;
		Agent agent = pq.getAgent();
		
		// create entry if non-existent, or add as existing multi-point bid
		if (!activeBids.containsKey(agent)) {
			PQBid newBid = new PQBid(pq.getAgent(), pq.getMarket(), null);
			newBid.addPoint(pq);
			activeBids.put(agent, newBid);
		} else {
			// don't need to add to FH since should already be there
			PQBid oldBid = (PQBid) activeBids.get(agent);
			PQBid newBid = new PQBid(oldBid);
			newBid.addPoint(pq);
			activeBids.put(agent, newBid);
		}
	}
	
	/**
	 * remove the active bid for this agentID
	 */
	public void removeBid(int agentID) {
		Integer key = new Integer(agentID);
		PQBid oldBid = (PQBid) activeBids.get(key);

		if (!activeBids.containsKey(key)) return;

		// remove all the PQPoints from the FourHeap
		for (Iterator<PQPoint> i = oldBid.bidTreeSet.iterator(); i.hasNext(); ) {
			FH.removeBid(i.next());
		}
		activeBids.remove(key);
	}

	@Override
	public int getDepth() {
		return this.FH.size();
	}	
	
	/**
	 * @return minimum price at which a winning buy could be placed
	 */
	public Bid getAskQuote() {
		PQPoint pq = FH.getAskQuote();
		
		PQBid b = null;
		if(pq.Parent != null) b = new PQBid(pq.getAgent(), pq.getMarket(), null);
		else b = new PQBid(null, null, null);
		
		b.addPoint(pq);
		return b;
	}

	/**
	 * @return maximum price at which a winning sell could be placed
	 */
	public Bid getBidQuote() {
		PQPoint pq = FH.getBidQuote();
		
		PQBid b = null;
		if(pq.Parent != null) b = new PQBid(pq.getAgent(), pq.getMarket(), null);
		else b = new PQBid(null, null, null);
		
		b.addPoint(pq);
		return b;
	}

	/**
	 * Prints the FH contents.
	 */
	public void logFourHeap(TimeStamp ts) {
		FH.logSets(ts);
	}
	
	/**
	 * Print the active bids in the orderbook.
	 */
	public void logActiveBids(TimeStamp ts) {
		String s = ts.toString() + " | " + this.market + " Active bids: ";
		for (Entry<Agent, Bid> entry : activeBids.entrySet()) {
			PQBid b = (PQBid) entry.getValue();
			for (Iterator<PQPoint> i = b.bidTreeSet.iterator(); i.hasNext(); ) {
				PQPoint pq = i.next();
				s += "(" + pq.getQuantity() + " " + pq.getPrice().toString() + ") ";
			}
		}
		log(INFO, s);
	}

	/**
	 * Print the cleared bids in the orderbook. 
	 */
	public void logClearedBids(TimeStamp ts) {
		String s = ts.toString() + " | " + this.market + " Cleared bids: ";
		for (Entry<Agent, Bid> entry : clearedBids.entrySet()) {
			PQBid b = (PQBid) entry.getValue();
			for (Iterator<PQPoint> i = b.bidTreeSet.iterator(); i.hasNext(); ) {
				PQPoint pq = i.next();
				s += "(" + pq.getQuantity() + " " + pq.getPrice().toString() + ") ";
			}
		}
		log(INFO, s);
	}

	
	/**
	 * @return array of bid IDs of the cleared bids
	 */
	public ArrayList<Integer> getClearedBidIDs() {
		if (!clearedBids.isEmpty()) {
			ArrayList<Integer> IDs = new ArrayList<Integer>();
			for (Entry<Agent, Bid> entry : clearedBids.entrySet()) {
				PQBid b = (PQBid) entry.getValue();
				IDs.add(b.getBidID());
			}
			return IDs;
		}
		return null;
	}
	
	
	/**
	 * Clears at the earliest price (given matching bids). For auction markets.
	 * 
	 * @param currentTime
	 * @param pricingPolicy between 0 and 1, default 0.5
	 * @return ArrayList of PQTransactions
	 */
	@Override
	public Collection<Transaction> earliestPriceClear(TimeStamp currentTime) {
		PQPoint buy, sell;
		clearedBids = new HashMap<Agent, Bid>();
		
		ArrayList<PQPoint> matchingBuys = new ArrayList<PQPoint>();
		ArrayList<PQPoint> matchingSells = new ArrayList<PQPoint>();
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();

		int buyAgentId = -1;
		int sellAgentId = -2;

		if (!FH.matchBuySet.isEmpty() && !FH.matchSellSet.isEmpty()) {
//			PQPoint b = (PQPoint) FH.matchBuySet.first();
			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgent().getID();
			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgent().getID();
		}
		if (buyAgentId != sellAgentId) {
			FH.clear(matchingBuys, matchingSells);
		}
		int numBuys = matchingBuys.size();
		int numSells = matchingSells.size();
		if (numBuys == 0) return Collections.emptySet();

		for (int i = 0, j = 0; i < numBuys || j < numSells;) {
			buy = matchingBuys.get(i);
			sell = matchingSells.get(j);
			int q = Math.min(buy.getQuantity(), Math.abs(sell.getQuantity()));
			Price p = PQPoint.earliestPrice(buy, sell);
			PQBid buyBid = buy.Parent;
			PQBid sellBid = sell.Parent;

			transactions.add(new PQTransaction(q, p, buy.getAgent(), sell.getAgent(), 
					buyBid, sellBid, currentTime, this.market));
//			transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts, marketID));
			log(INFO, currentTime + " | " + this.market + 
					" Quantity=" + q + " cleared at Price=" + p.getPrice());

			Agent key = buy.getAgent();
			if (!clearedBids.containsKey(key)) {
				clearedBids.put(key, buy.Parent);
				// activeBids.remove(key);
			}
			key = sell.getAgent();
			if (!clearedBids.containsKey(key)) {
				clearedBids.put(key, sell.Parent);
				// activeBids.remove(key);
			}

			buy.transact(q);
			sell.transact(-1 * q);
			// Update MB/MS lists
			if (buy.getQuantity() == 0) i++;
			if (sell.getQuantity() == 0) j++;
		}
		return transactions;
	}



	/**
	 * Clears at a uniform price. For call markets.
	 * 
	 * @param ts
	 * @param pricingPolicy between 0 and 1, default 0.5
	 * @return ArrayList of PQTransactions
	 */
	@Override
	public ArrayList<Transaction> uniformPriceClear(TimeStamp ts, float pricingPolicy) {

		PQPoint buy, sell;
		clearedBids = new HashMap<Agent, Bid>();

		ArrayList<PQPoint> matchingBuys = new ArrayList<PQPoint>();
		ArrayList<PQPoint> matchingSells = new ArrayList<PQPoint>();
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();

		int buyAgentId = -1;
		int sellAgentId = -2;

		if (!FH.matchBuySet.isEmpty() && !FH.matchSellSet.isEmpty()) {
			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgent().getID();
			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgent().getID();
		}
		if (buyAgentId != sellAgentId) {
			FH.clear(matchingBuys, matchingSells);
		}
		
		int numBuys = matchingBuys.size();
		int numSells = matchingSells.size();
		if (numBuys == 0) return null;
		
		Price p = new Price(0);
		for (int i = 0, j = 0; i < numBuys || j < numSells;) {
			buy = (PQPoint) matchingBuys.get(i);
			sell = (PQPoint) matchingSells.get(j);
			int q = Math.min(buy.getQuantity(), Math.abs(sell.getQuantity()));
			PQBid buyBid = buy.Parent;
			PQBid sellBid = sell.Parent;
			
			// Assign price if not assigned yet
			// Get price by taking first of the matching buys/sells
			if (p.getPrice() == 0) {
//				TreeSet<PQPoint> b = new TreeSet<PQPoint>(matchingBuys);
//				TreeSet<PQPoint> a = new TreeSet<PQPoint>(matchingSells);
//				PQPoint bid = b.first();
//				PQPoint ask = a.last();
				PQPoint bid = ((PQBid) getBidQuote()).bidTreeSet.first();
				PQPoint ask = ((PQBid) getAskQuote()).bidTreeSet.first();
				
				p = new Price(Math.round((ask.getPrice().getPrice() - 
						bid.getPrice().getPrice()) * pricingPolicy + bid.getPrice().getPrice()));
				p = new Price(MathUtils.quantize(p.getPrice(), data.tickSize));
				log(INFO, ts + " | " + market + 
						" clearing price based on (BID: " + 
						bid.getPrice().getPrice() + ", ASK:" + ask.getPrice().getPrice() + 
						") & pricingPolicy=" + pricingPolicy + " => price " + p.getPrice());
			}
			transactions.add(new PQTransaction(q, p, buy.getAgent(), sell.getAgent(), 
					buyBid, sellBid, ts, market));
//			transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts, marketID));
			log(INFO, ts + " | " + market + 
					" Quantity=" + q + " cleared at Price=" + p.getPrice());
			
			Agent key = buy.getAgent();
			if (!clearedBids.containsKey(key))
				clearedBids.put(key, buy.Parent);
			key = sell.getAgent();
			if (!clearedBids.containsKey(key))
				clearedBids.put(key, sell.Parent);

			buy.transact(q);
			sell.transact(-1 * q);
			if (buy.getQuantity() == 0) i++;
			if (sell.getQuantity() == 0) j++;
		}
		return transactions;
	}

	
//	/**
//	 * Fixes issue caused by multi-unit bids where a partially-transacted bid
//	 * remains in the clearedBids lists. Copies bids with non-zero quantities
//	 * in clearedBids back to activeBids.
//	 */
//	private void updateActiveBids() {
//		// cycle through the list of bids in cleared to check for ones with unit=1
//		for (Map.Entry<Integer,Bid> entry : clearedBids.entrySet()) {
//			PQBid b = (PQBid) entry.getValue();
//			for (Iterator<PQPoint> i = b.bidTreeSet.iterator(); i.hasNext(); ) {
//				PQPoint pq = i.next();
//				
//				if (pq.getQuantity() != 0) {
//					// need to add to active bids
//					insertPoint(pq);
//					// don't need to remove from cleared bids because that resets
//				}	
//			}
//		}
//	}
//
//	/**
//	 * Clear the orderbook. If there is tie between bids, choose the one with the
//	 * earlier price.
//	 * 
//	 * @param ts
//	 * @return
//	 */
//	public ArrayList<Transaction> earliestPriceClear(TimeStamp ts) {
//		PQPoint buy, sell;
//		clearedBids = null;
//
//		ArrayList<PQPoint> matchingBuys = new ArrayList<PQPoint>();
//		ArrayList<PQPoint> matchingSells = new ArrayList<PQPoint>();
//		ArrayList<Transaction> transactions = null;
//
//		int buyAgentId = -1;
//		int sellAgentId = -2;
//
//		if (!FH.matchBuySet.isEmpty() && !FH.matchSellSet.isEmpty()) {
////			PQPoint b = (PQPoint) FH.matchBuySet.first();
//			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgentID();
//			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgentID();
//		}
//		if (buyAgentId != sellAgentId) {
//			FH.clear(matchingBuys, matchingSells);
//		}
//
//		int numBuys = matchingBuys.size();
//		int numSells = matchingSells.size();
//		if (numBuys == 0) return null;
//
//		clearedBids = new HashMap<Integer,Bid>();
//		
//		while (!matchingBuys.isEmpty() && !matchingSells.isEmpty()) {
//			buy = null;
//			sell = null;
//			clearedBids = new HashMap<Integer,Bid>();
//			transactions = new ArrayList<Transaction>();
//			int i, j;	// i is index for buys, j is index for sells
//			
//			// More buys through sells, so go through sells first
//			if (numBuys != numSells)
//				i = 0;
//			if (numBuys >= numSells) {
//				sell = matchingSells.get(numSells-1);
//				j = numSells-1;
//				for (i = numBuys-1; i >= 0; i--) {
//					if (matchingBuys.get(i).getAgentID() != sell.getAgentID()) {
//						buy = matchingBuys.get(i);
//						break;
//					}
//				}
//			} else { // More sells than buys, so go through buys first
//				buy = matchingBuys.get(numBuys-1);
//				i = numBuys-1;
//				for (j = numSells-1; j >= 0; j--) {
//					if (matchingSells.get(j).getAgentID() != buy.getAgentID()) {
//						sell = matchingSells.get(j);
//						break;
//					}
//				}
//			}
//			if (buy == null || sell == null) {
//				// due to agentIDs being the same, exit while loop
//				break;
//			}
//			
//			int q = Math.min(buy.getQuantity(), Math.abs(sell.getQuantity()));
//			Price p = PQPoint.earliestPrice(buy, sell);
//			transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts, marketID));
//
//			Integer key = new Integer(buy.getAgentID());
//			if (!clearedBids.containsKey(key)) {
//				clearedBids.put(key, buy.Parent);
////				activeBids.remove(key);
//			}
//			key = new Integer(sell.getAgentID());
//			if (!clearedBids.containsKey(key)) {
//				clearedBids.put(key, sell.Parent);
////				activeBids.remove(key);
//			}
//			
//			buy.transact(q);
//			sell.transact(-1 * q);
//			// Update MB/MS lists
//			if (buy.getQuantity() == 0) matchingBuys.remove(i);
//			if (sell.getQuantity() == 0) matchingSells.remove(j);
//		}
//		return transactions;
//	}
	
	
}


