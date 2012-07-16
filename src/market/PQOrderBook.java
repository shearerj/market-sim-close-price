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

		ArrayList matchingBuys = new ArrayList();
		ArrayList matchingSells = new ArrayList();
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();

		int buyAgentId = -1;
		int sellAgentId = -2;

		if (!FH.matchBuySet.isEmpty() && !FH.matchSellSet.isEmpty()) {
			PQPoint b = (PQPoint) FH.matchBuySet.first();
			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgentID();
			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgentID();
		}
		/* crufty XXX hack to prevent same-bid execution */
		if (buyAgentId != sellAgentId) {
			FH.clear(matchingBuys, matchingSells);
		}

		int numBuys = matchingBuys.size();
		int numSells = matchingSells.size();
		if (numBuys == 0) return null;

		clearedBids = new HashMap<Integer,Bid>();
		for (int i = 0, j = 0; i < numBuys || j < numSells;) {
			buy = (PQPoint) matchingBuys.get(i);
			sell = (PQPoint) matchingSells.get(j);
			int q = Math.min(buy.getQuantity(), Math.abs(sell.getQuantity()));
			Price p = PQPoint.earliestPrice(buy, sell);

			if (buy.getAgentID() != sell.getAgentID()) {
				transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts, marketID));

				Integer key = new Integer(buy.getAgentID());
				if (!clearedBids.containsKey(key)) clearedBids.put(key, buy.Parent);
				key = new Integer(sell.getAgentID());
				if (!clearedBids.containsKey(key)) clearedBids.put(key, sell.Parent);

				//System.out.println("transacted something");
				buy.transact(q);
				sell.transact(-1 * q);
				if (buy.getQuantity() == 0) i++;
				if (sell.getQuantity() == 0) j++;
			}
			assert ((i < numBuys && j < numSells) || (i == numBuys && j == numSells)) : "earliest price clear broken";
		}
		return transactions;
	}
}
