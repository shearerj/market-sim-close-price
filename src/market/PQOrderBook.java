package market;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
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
	protected final int tickSize;

	public PQOrderBook(Market market) {
		super(market);
		this.FH = new FourHeap(market);
		this.tickSize = 1; // FIXME
	}

	/**
	 * inserts new bid into the order book
	 */
	public void insertBid(Bid newBid) {
		Agent agent = newBid.getAgent();

		// create entry if non-existent, remove old bid if exists
		if (!activeBids.containsKey(agent)) {
			activeBids.put(agent, newBid);
		} else {
			Bid oldBid = activeBids.get(agent);
			for (Iterator<PQPoint> i = oldBid.bidTreeSet.iterator(); i.hasNext();) {
				FH.removeBid(i.next());
			}
			activeBids.put(agent, newBid);
		}
		Bid pqBid = newBid;
		// insert all new Points from this Bid into the 4Heap
		for (Iterator<PQPoint> i = pqBid.bidTreeSet.iterator(); i.hasNext();) {
			PQPoint p = i.next();
			p.Parent = pqBid;
			FH.insertBid(p);
		}
	}

	/**
	 * inserts a pq point into the order book, but doesn't update FH. For updating activeBids list
	 * in clear method.
	 * 
	 * @param newPoint
	 */
	public void insertPoint(Point newPoint) {
		PQPoint pq = (PQPoint) newPoint;
		Agent agent = pq.getAgent();

		// create entry if non-existent, or add as existing multi-point bid
		if (!activeBids.containsKey(agent)) {
			Bid newBid = new Bid(pq.getAgent(), pq.getMarket(), null);
			newBid.addPoint(pq);
			activeBids.put(agent, newBid);
		} else {
			// don't need to add to FH since should already be there
			Bid oldBid = activeBids.get(agent);
			oldBid.addPoint(pq);
			activeBids.put(agent, oldBid);
		}
	}

	/**
	 * remove the active bid for this agentID
	 */
	public void removeBid(int agentID) {
		Integer key = new Integer(agentID);
		Bid oldBid = activeBids.get(key);

		if (!activeBids.containsKey(key)) return;

		// remove all the PQPoints from the FourHeap
		for (Iterator<PQPoint> i = oldBid.bidTreeSet.iterator(); i.hasNext();) {
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

		Bid b = null;
		if (pq.Parent != null)
			b = new Bid(pq.getAgent(), pq.getMarket(), null);
		else
			b = new Bid(null, null, null);

		b.addPoint(pq);
		return b;
	}

	/**
	 * @return maximum price at which a winning sell could be placed
	 */
	public Bid getBidQuote() {
		PQPoint pq = FH.getBidQuote();

		Bid b = null;
		if (pq.Parent != null)
			b = new Bid(pq.getAgent(), pq.getMarket(), null);
		else
			b = new Bid(null, null, null);

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
			Bid b = entry.getValue();
			for (Iterator<PQPoint> i = b.bidTreeSet.iterator(); i.hasNext();) {
				PQPoint pq = i.next();
				s += "(" + pq.getQuantity() + " " + pq.getPrice().toString()
						+ ") ";
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
			Bid b = entry.getValue();
			for (Iterator<PQPoint> i = b.bidTreeSet.iterator(); i.hasNext();) {
				PQPoint pq = i.next();
				s += "(" + pq.getQuantity() + " " + pq.getPrice().toString()
						+ ") ";
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
				Bid b = entry.getValue();
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
	 * @param pricingPolicy
	 *            between 0 and 1, default 0.5
	 * @return ArrayList of PQTransactions
	 */
	@Override
	public List<Transaction> earliestPriceClear(TimeStamp currentTime) {
		PQPoint buy, sell;
		clearedBids = new HashMap<Agent, Bid>();

		ArrayList<PQPoint> matchingBuys = new ArrayList<PQPoint>();
		ArrayList<PQPoint> matchingSells = new ArrayList<PQPoint>();
		ArrayList<Transaction> transactions = new ArrayList<Transaction>();

		int buyAgentId = -1;
		int sellAgentId = -2;

		if (!FH.matchBuySet.isEmpty() && !FH.matchSellSet.isEmpty()) {
			// PQPoint b = (PQPoint) FH.matchBuySet.first();
			buyAgentId = ((PQPoint) FH.matchBuySet.first()).getAgent().getID();
			sellAgentId = ((PQPoint) FH.matchSellSet.first()).getAgent().getID();
		}
		if (buyAgentId != sellAgentId) {
			FH.clear(matchingBuys, matchingSells);
		}
		int numBuys = matchingBuys.size();
		int numSells = matchingSells.size();
		if (numBuys == 0) return Collections.emptyList();

		for (int i = 0, j = 0; i < numBuys || j < numSells;) {
			buy = matchingBuys.get(i);
			sell = matchingSells.get(j);
			int q = Math.min(buy.getQuantity(), Math.abs(sell.getQuantity()));
			Price p = PQPoint.earliestPrice(buy, sell);
			Bid buyBid = buy.Parent;
			Bid sellBid = sell.Parent;

			transactions.add(new PQTransaction(q, p, buy.getAgent(),
					sell.getAgent(), buyBid, sellBid, currentTime, this.market));
			// transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts,
			// marketID));
			log(INFO, currentTime + " | " + this.market + " Quantity=" + q
					+ " cleared at Price=" + p.getPrice());

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
	 * @param pricingPolicy
	 *            between 0 and 1, default 0.5
	 * @return ArrayList of PQTransactions
	 */
	@Override
	public ArrayList<Transaction> uniformPriceClear(TimeStamp ts,
			float pricingPolicy) {

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
			Bid buyBid = buy.Parent;
			Bid sellBid = sell.Parent;

			// Assign price if not assigned yet
			// Get price by taking first of the matching buys/sells
			if (p.getPrice() == 0) {
				// TreeSet<PQPoint> b = new TreeSet<PQPoint>(matchingBuys);
				// TreeSet<PQPoint> a = new TreeSet<PQPoint>(matchingSells);
				// PQPoint bid = b.first();
				// PQPoint ask = a.last();
				PQPoint bid = getBidQuote().bidTreeSet.first();
				PQPoint ask = getAskQuote().bidTreeSet.first();

				p = new Price(
						Math.round((ask.getPrice().getPrice() - bid.getPrice().getPrice())
								* pricingPolicy + bid.getPrice().getPrice())).quantize(tickSize);
				log(INFO, ts + " | " + market
						+ " clearing price based on (BID: "
						+ bid.getPrice().getPrice() + ", ASK:"
						+ ask.getPrice().getPrice() + ") & pricingPolicy="
						+ pricingPolicy + " => price " + p.getPrice());
			}
			transactions.add(new PQTransaction(q, p, buy.getAgent(),
					sell.getAgent(), buyBid, sellBid, ts, market));
			// transactions.add(new PQTransaction(q, p, buy.getAgentID(), sell.getAgentID(), ts,
			// marketID));
			log(INFO, ts + " | " + market + " Quantity=" + q
					+ " cleared at Price=" + p.getPrice());

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

}
