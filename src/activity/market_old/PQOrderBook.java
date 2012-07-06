/*
 *  $Id: PQOrderBook.java,v 1.16 2005/03/29 18:19:54 chengsf Exp $
 */
package activity.market;

import systemmanager.Log;
import event.TimeStamp;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.ArrayList;

public class PQOrderBook extends OrderBook {
	public FourHeap FH;
	public Log log;

	public PQOrderBook() {
		super();
		FH = new FourHeap();
	}

	/**
	 * create orderbook with size entries
	 *
	 * @param size
	 */
	public PQOrderBook(int size) {
		super(size);
		FH = new FourHeap();
	}

	/**
	 * create orderbook with default num entries
	 */
	public PQOrderBook(MarketConfig config) {
		super(config);
		FH = new FourHeap();
	}

	/**
	 * set the log/aucID for logging
	 *
	 * @param l   log
	 * @param aID auction ID
	 */
	public void setParams(Log l, int aID) {
		log = l;
		aucID = aID;
		FH.setParams(l, aID);
	}

	/**
	 * clear out all bids from the orderbook
	 */

	public void reset() {
		dataEntries = new HashMap<Integer,OrderBook.AgentDataEntry>();
		FH = new FourHeap();
	}

	public Bid lastAlloc(int agent_id) {
		OrderBook.AgentDataEntry ade = dataEntries.get(new Integer(agent_id));
		if (ade == null) {
			debugn("not found");
			return null;
		} else
			return ade.alloc;
	}

	/**
	 * @return the real-time allocation for this agent
	 */
	public Bid realTimeAlloc(int agent_id) {
		int tempalloc = 0;
		OrderBook.AgentDataEntry ade = dataEntries.get(new Integer(agent_id));
		if (ade == null) {
			debugn("not found");
			return null;
		} else {
			//sum the allocs of all PQPoints
			for (int i = 0; i < ((PQBid) ade.bid).bidArray.length; i++) {
				tempalloc += FH.pointAlloc(((PQBid) ade.bid).bidArray[i]);
			}
		}
		PQBid alloc = new PQBid();
		alloc.addPoint(tempalloc, new Price(0));
		return alloc;
	}

	/**
	 * @return hashtable of real time allocations for all agents with bids
	 *         compute allocations for all agents having an active bid
	 *         in the OrderBook
	 *         allocations are saved into their OrderBook entries
	 */
	public HashMap<Integer,Bid> computeAllocations() {
		Integer agentid;
		HashMap<Integer,Bid> hm = new HashMap<Integer,Bid>(dataEntries.size());
		for (Enumeration e = (Enumeration) dataEntries.keySet(); e.hasMoreElements();) {
			agentid = (Integer) e.nextElement();
			OrderBook.AgentDataEntry ade = dataEntries.get(agentid);
			PQBid B = (PQBid) ade.bid;
			ade.alloc = new PQBid();
			int q = 0;
			for (int i = 0; i < B.bidArray.length; i++) {
				q += FH.pointAlloc(B.bidArray[i]);
			}
			((PQBid) ade.alloc).addPoint(q, new Price(0));
			hm.put(agentid, ade.alloc);
		}
		return hm;
	}

	/**
	 * @return hashtable of last-computed allocations
	 */
	public HashMap<Integer,Bid> getAllocations() {
		Integer agentid;
		HashMap<Integer,Bid> hm = new HashMap<Integer,Bid>();

		for (Enumeration e = (Enumeration) dataEntries.keySet(); e.hasMoreElements();) {
			agentid = (Integer) e.nextElement();
			hm.put(agentid, dataEntries.get(agentid).alloc);
		}
		return hm;
	}

	/**
	 * @return Vector of PQTransactions
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
			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgentID();
			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgentID();
		}
		/* crufty XXX hack to prevent same-bid execution */
		if (buyAgentId != sellAgentId) {
			FH.clear(matchingBuys, matchingSells);
		}

		if ((matchingBuys == null) || (matchingSells == null))
			debugn("error");

		int numBuys = matchingBuys.size();
		int numSells = matchingSells.size();

		if (numBuys == 0)
			return null;

		clearedBids = new HashMap<Integer,Bid>();

		for (int i = 0, j = 0; i < numBuys || j < numSells;) {
			buy = (PQPoint) matchingBuys.get(i);
			sell = (PQPoint) matchingSells.get(j);
			int q = Math.min(buy.getquantity(), Math.abs(sell.getquantity()));
			Price p = PQPoint.earliestPrice(buy, sell);

			if (buy.getAgentID() != sell.getAgentID()) {
				transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts, aucID));

				Integer key = new Integer(buy.getAgentID());
				if (!clearedBids.containsKey(key))
					clearedBids.put(key, buy.Parent);
				key = new Integer(sell.getAgentID());
				if (!clearedBids.containsKey(key))
					clearedBids.put(key, sell.Parent);

				//System.out.println("transacted something");
				buy.transact(q);
				sell.transact(-1 * q);
				if (buy.getquantity() == 0) i++;
				if (sell.getquantity() == 0) j++;
			}
			/*  assert ((i < numBuys && j < numSells) ||
      (i == numBuys && j == numSells)) : "uniform clear broken";*/

		}
		return transactions;
	}

	//clear auction, return vector of transactions (or just log them?)
	/**
	 * @return Vector of PQTransactions
	 */
	public ArrayList<Transaction> uniformPriceClear(TimeStamp ts, float pricing_k) {
		long init = new TimeStamp().longValue();

		PQBid bid = (PQBid) getBidQuote();
		PQBid ask = (PQBid) getAskQuote();
		log(Log.INFO, "clear, bid/ask" + bid.quoteString() + "/" + ask.quoteString());
		Price p = bid.bidArray[0].price;
		clearedBids = new HashMap<Integer,Bid>();

		PQPoint buy, sell;

		ArrayList matchingBuys = new ArrayList();
		ArrayList matchingSells = new ArrayList();
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();

		int buyAgentId = -1;
		int sellAgentId = -2;
		if (!FH.matchBuySet.isEmpty() && !FH.matchSellSet.isEmpty()) {
			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgentID();
			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgentID();
		}
		/* crufty XXX hack to prevent same-bid execution */
		if (buyAgentId != sellAgentId) {
			FH.clear(matchingBuys, matchingSells);
		}

		if ((matchingBuys == null) || (matchingSells == null))
			debugn("error with FH in OrderBook.uniformPriceClear");

		int numBuys = matchingBuys.size();
		int numSells = matchingSells.size();

		if (numBuys == 0)
			return null;

		for (int i = 0, j = 0; i < numBuys || j < numSells;) {
			buy = (PQPoint) matchingBuys.get(i);
			sell = (PQPoint) matchingSells.get(j);
			
			int q = Math.min(buy.getquantity(), Math.abs(sell.getquantity()));
			transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts, aucID));
			Integer key = new Integer(buy.getAgentID());
			if (!clearedBids.containsKey(key))
				clearedBids.put(key, buy.Parent);
			key = new Integer(sell.getAgentID());
			if (!clearedBids.containsKey(key))
				clearedBids.put(key, sell.Parent);

			buy.transact(q);
			sell.transact(-1 * q);
			if (buy.getquantity() == 0) i++;
			if (sell.getquantity() == 0) j++;

			/*assert ((i < numBuys && j < numSells) ||
      (i == numBuys && j == numSells)) : "uniform clear broken";
			 */
		}
		log(Log.INFO, "TIME: Clearing took " + ((new TimeStamp().longValue()) - init) + " milliseconds");
		return transactions;
	}


	public ArrayList<Bid> getQuote() {
		ArrayList<Bid> quote = new ArrayList<Bid>(2);
		quote.add(getBidQuote());
		quote.add(getAskQuote());
		return quote;
	}
	
	public ArrayList<Bid> getQuote(float delta) {
		if (delta == 0) return getQuote();
		ArrayList<Bid> quote = new ArrayList<Bid>(2);
		quote.add(getBidQuote(delta));
		quote.add(getAskQuote(delta));
		return quote;
	}
	
	/**
	 * Hypothetical quantity won, hashed by agent ID.
	 * 
	 * @return HashMap
	 */
	public HashMap<Integer,Integer> getAgentQuote() {
		Integer agentID;
		HashMap<Integer,Integer> hm = new HashMap<Integer,Integer>(dataEntries.size());
		for (Enumeration e = (Enumeration) dataEntries.keySet(); e.hasMoreElements();) {
			agentID = (Integer) e.nextElement();
			OrderBook.AgentDataEntry ade = dataEntries.get(agentID);
			PQBid B = (PQBid) ade.bid;
			ade.alloc = new PQBid();
			int q = 0;
			for (int i = 0; i < B.bidArray.length; i++) {
				q += FH.pointAlloc(B.bidArray[i]);
			}
			((PQBid) ade.alloc).addPoint(q, new Price(0));
			hm.put(agentID, q);
		}
		return hm;
	}
	
	/**
	 * @return maximum price at which a winning sell could be placed
	 */
	public Bid getBidQuote() {
		PQBid b = new PQBid();
		b.addPoint(0, FH.getBidQuote());
		return b;
	}

	public Bid getBidQuote(float delta) {
		PQBid b = new PQBid();
		b.addPoint(0, new Price(FH.getAskQuote().price + delta));
		return b;
	}

	/**
	 * @return minimum price at which a winning buy could be placed
	 */
	public Bid getAskQuote() {
		PQBid b = new PQBid();
		b.addPoint(0, FH.getAskQuote());
		return b;
	}

	public Bid getAskQuote(float delta) {
		PQBid b = new PQBid();
		b.addPoint(0, new Price(FH.getAskQuote().price + delta));
		return b;
	}

	/**
	 * we're going to assume that a bid is divisible for now
	 * and assume that the bid is fully verified
	 */
	public void insertBid(Bid newBid) {
		long init = new TimeStamp().longValue();
		//super.insertBid(newBid);
		Integer key = newBid.agentID;
		// look up the agent's entry
		OrderBook.AgentDataEntry ade = dataEntries.get(key);
		//create entry if non-existent, remove old bid if exists
		if (ade == null) {
			log(Log.INFO, "insertBid, agent id " + newBid.agentID);
			ade = new OrderBook.AgentDataEntry();
			dataEntries.put(key, ade);
		} else {
			PQBid oldBid = (PQBid) ade.bid;
			for (int i = 0; i < oldBid.bidArray.length; i++) {
				FH.removeBid(oldBid.bidArray[i]);
			}
		}
		ade.bid = newBid;
		PQBid pqBid = (PQBid) newBid;
		//insert all new Points from this Bid into the 4Heap
		for (int i = 0; i < pqBid.bidArray.length; i++) {
			FH.insertBid(pqBid.bidArray[i]);
		}
		log(Log.INFO, "TIME: Insertion took " + ((new TimeStamp().longValue()) - init) + " milliseconds");
	}

	/**
	 * remove the active bid for this agent_id
	 */
	public void removeBid(int agentID) {
		long init = new TimeStamp().longValue();
		Integer key = new Integer(agentID);
		AgentDataEntry ade = dataEntries.get(key);
		if (ade == null) {
			return;
		}
		PQBid oldBid = (PQBid) ade.bid;

		//remove all the PQPoints from the fourheap
		for (int i = 0; i < oldBid.bidArray.length; i++) {
			debug(oldBid.bidArray[i].toQPString());
			FH.removeBid(oldBid.bidArray[i]);
		}
		//agent no longer has a bid in auction, so remove dataEntries entry
		dataEntries.remove(key);
		log(Log.INFO, "TIME: Removal took " + ((new TimeStamp().longValue()) - init) + " milliseconds");
	}


	public void logBids() {
		Integer agentid;
		for (Enumeration e = (Enumeration) dataEntries.keySet(); e.hasMoreElements();) {
			agentid = (Integer) e.nextElement();
			String s = aucID + "/" + agentid;
			PQBid B = (PQBid) dataEntries.get(agentid).bid;
			for (int i = 0; i < B.bidArray.length; i++) {
				s += "/" + B.bidArray[i].getprice() + "/" + B.bidArray[i].getquantity();
			}
			FH.logSets();
		}
	}

//	public Vector BBVickreyPriceClear(TimeStamp ts) {
//		return null;
//	}

}

