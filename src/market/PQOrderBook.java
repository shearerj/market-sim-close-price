package market;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;

import event.TimeStamp;
import systemmanager.*;

/**
 * @author ewah
 */
public class PQOrderBook extends OrderBook {

	public FourHeap FH;

	public PQOrderBook(int mktID) {
		super(mktID);
		FH = new FourHeap(mktID);
	}

	public void setParams(Log l, int mktID) {
		log = l;
		marketID = mktID;
		FH.setParams(l, mktID);
	}

	public void insertBid(Bid newBid) {
		Integer key = newBid.agentID;

		// create entry if non-existent, remove old bid if exists
		if (!activeBids.containsKey(key)) {
			activeBids.put(key, (PQBid) newBid);
		} else {
			PQBid oldBid = (PQBid) activeBids.get(key);
			for (Iterator<PQPoint> i = oldBid.bidTreeSet.iterator(); i.hasNext(); ) {
				FH.removeBid(i.next());
			}
			activeBids.put(key, (PQBid) newBid);
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
	 * remove the active bid for this agent_id
	 */
	public void removeBid(int agentID) {
		Integer key = new Integer(agentID);
		PQBid oldBid = (PQBid) activeBids.get(key);

		if (!activeBids.containsKey(key)) return;

		//remove all the PQPoints from the fourheap
		for (Iterator<PQPoint> i = oldBid.bidTreeSet.iterator(); i.hasNext(); ) {
			FH.removeBid(i.next());
		}
		activeBids.remove(key);
	}

	/**
	 * @return minimum price at which a winning buy could be placed
	 */
	public Bid getAskQuote() {
		PQBid b = new PQBid();
		b.addPoint(0, FH.getAskQuote());
		return b;
	}

	/**
	 * @return maximum price at which a winning sell could be placed
	 */
	public Bid getBidQuote() {
		PQBid b = new PQBid();
		b.addPoint(0, FH.getBidQuote());
		return b;
	}

	/**
	 * Print the active bids in the orderbook.
	 */
	public void logActiveBids() {

		String s = "Active bids: ";
		for (Map.Entry<Integer,Bid> entry : activeBids.entrySet()) {
			PQBid b = (PQBid) entry.getValue();
			for (Iterator<PQPoint> i = b.bidTreeSet.iterator(); i.hasNext(); ) {
				PQPoint pq = i.next();
				s += "(" + pq.getQuantity() + " " + pq.getPrice().toString() + ") ";
			}
		}
		FH.logSets();
		System.out.println(s);
	}

	/**
	 * Print the cleared bids in the orderbook. 
	 */
	public void logClearedBids() {
		String s = "Cleared bids: ";
		for (Map.Entry<Integer,Bid> entry : clearedBids.entrySet()) {
			PQBid b = (PQBid) entry.getValue();
			for (Iterator<PQPoint> i = b.bidTreeSet.iterator(); i.hasNext(); ) {
				PQPoint pq = i.next();
				s += "(" + pq.getQuantity() + " " + pq.getPrice().toString() + ") ";
			}
		}
		FH.logSets();
		System.out.println(s);
	}


	/**
	 * Clear the orderbook. If there is tie between bids, choose the one with the
	 * earlier price.
	 * 
	 * @param ts
	 * @return
	 */
	public ArrayList<Transaction> earliestPriceClear(TimeStamp ts) {
		PQPoint buy, sell;
		clearedBids = null;

		ArrayList<PQPoint> matchingBuys = new ArrayList<PQPoint>();
		ArrayList<PQPoint> matchingSells = new ArrayList<PQPoint>();
		ArrayList<Transaction> transactions = null;

		int buyAgentId = -1;
		int sellAgentId = -2;

		if (!FH.matchBuySet.isEmpty() && !FH.matchSellSet.isEmpty()) {
//			PQPoint b = (PQPoint) FH.matchBuySet.first();
			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgentID();
			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgentID();
		}
		if (buyAgentId != sellAgentId) {
			FH.clear(matchingBuys, matchingSells);
		}
		
		while (!matchingBuys.isEmpty() && !matchingSells.isEmpty()) {
			int numBuys = matchingBuys.size();
			int numSells = matchingSells.size();
			
			buy = null;
			sell = null;
			clearedBids = new HashMap<Integer,Bid>();
			transactions = new ArrayList<Transaction>();
			int i, j;	// i is index for buys, j is index for sells
			
			// More buys through sells, so go through sells first
			if (numBuys != numSells)
				i = 0;
			if (numBuys >= numSells) {
				sell = matchingSells.get(numSells-1);
				j = numSells-1;
				for (i = numBuys-1; i >= 0; i--) {
					if (matchingBuys.get(i).getAgentID() != sell.getAgentID()) {
						buy = matchingBuys.get(i);
						break;
					}
				}
			} else { // More sells than buys, so go through buys first
				buy = matchingBuys.get(numBuys-1);
				i = numBuys-1;
				for (j = numSells-1; j >= 0; j--) {
					if (matchingSells.get(j).getAgentID() != buy.getAgentID()) {
						sell = matchingSells.get(j);
						break;
					}
				}
			}
			if (buy == null || sell == null) {
				// due to agentIDs being the same, exit while loop
				break;
			}
			int q = Math.min(buy.getQuantity(), Math.abs(sell.getQuantity()));
			Price p = PQPoint.earliestPrice(buy, sell);
			transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts, marketID));

			Integer key = new Integer(buy.getAgentID());
			if (!clearedBids.containsKey(key)) {
				clearedBids.put(key, buy.Parent);
				activeBids.remove(key);
			}
			key = new Integer(sell.getAgentID());
			if (!clearedBids.containsKey(key)) {
				clearedBids.put(key, sell.Parent);
				activeBids.remove(key);
			}
			
			buy.transact(q);
			sell.transact(-1 * q);
			// Update MB/MS lists
			if (buy.getQuantity() == 0) matchingBuys.remove(i);
			if (sell.getQuantity() == 0) matchingSells.remove(j);
		}
		return transactions;
	}

	/**
	 * Clears at a uniform price. For call markets.
	 * 
	 * @return ArrayList of PQTransactions
	 */
	public ArrayList<Transaction> uniformPriceClear(TimeStamp ts) {
		long init = new TimeStamp().longValue();

		PQBid bid = (PQBid) getBidQuote();
		PQBid ask = (PQBid) getAskQuote();
//		log(Log.INFO, "clear, bid/ask" + bid.quoteString() + "/" + ask.quoteString());
		Price p = bid.bidTreeSet.first().price; // highest bid price

		PQPoint buy, sell;
		clearedBids = null;

		ArrayList<PQPoint> matchingBuys = new ArrayList<PQPoint>();
		ArrayList<PQPoint> matchingSells = new ArrayList<PQPoint>();
		ArrayList<Transaction> transactions = null;

		int buyAgentId = -1;
		int sellAgentId = -2;

		if (!FH.matchBuySet.isEmpty() && !FH.matchSellSet.isEmpty()) {
			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgentID();
			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgentID();
		}
		if (buyAgentId != sellAgentId) {
			FH.clear(matchingBuys, matchingSells);
		}

		while (!matchingBuys.isEmpty() && !matchingSells.isEmpty()) {
			int numBuys = matchingBuys.size();
			int numSells = matchingSells.size();
			
			buy = null;
			sell = null;
			clearedBids = new HashMap<Integer,Bid>();
			transactions = new ArrayList<Transaction>();
			int i, j;	// i is index for buys, j is index for sells
			
			// More buys through sells, so go through sells first
			if (numBuys != numSells)
				i = 0;
			if (numBuys >= numSells) {
				sell = matchingSells.get(numSells-1);
				j = numSells-1;
				for (i = numBuys-1; i >= 0; i--) {
					if (matchingBuys.get(i).getAgentID() != sell.getAgentID()) {
						buy = matchingBuys.get(i);
						break;
					}
				}
			} else { // More sells than buys, so go through buys first
				buy = matchingBuys.get(numBuys-1);
				i = numBuys-1;
				for (j = numSells-1; j >= 0; j--) {
					if (matchingSells.get(j).getAgentID() != buy.getAgentID()) {
						sell = matchingSells.get(j);
						break;
					}
				}
			}
			if (buy == null || sell == null) {
				// due to agentIDs being the same, exit while loop
				break;
			}
			int q = Math.min(buy.getQuantity(), Math.abs(sell.getQuantity()));
			transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts, marketID));

			Integer key = new Integer(buy.getAgentID());
			if (!clearedBids.containsKey(key)) {
				clearedBids.put(key, buy.Parent);
				activeBids.remove(key);
			}
			key = new Integer(sell.getAgentID());
			if (!clearedBids.containsKey(key)) {
				clearedBids.put(key, sell.Parent);
				activeBids.remove(key);
			}
			
			buy.transact(q);
			sell.transact(-1 * q);
			// Update MB/MS lists
			if (buy.getQuantity() == 0) matchingBuys.remove(i);
			if (sell.getQuantity() == 0) matchingSells.remove(j);
		}
		return transactions;
	}

	
//	/**
//	 * Fixes issue caused by multi-unit bids where a partially-transacted bid
//	 * remains in the clearedBids lists. Moves bids with non-zero quantities
//	 * in clearedBids back to activeBids.
//	 */
//	private void fixBidLists() {
//		// cycle through the list of bids in cleared to check for ones with unit=1
//		for (Map.Entry<Integer,Bid> entry : clearedBids.entrySet()) {
//			PQBid b = (PQBid) entry.getValue();
//			for (Iterator<PQPoint> i = b.bidTreeSet.iterator(); i.hasNext(); ) {
//				PQPoint pq = i.next();
//				if (pq.getQuantity() != 0) {
//					
//				}
////				s += "(" + pq.getQuantity() + " " + pq.getPrice().toString() + ") ";
//			}
//		}
//	}
}
