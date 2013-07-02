package market;

import java.util.Iterator;
import java.util.TreeSet;

import entity.Agent;
import entity.Market;
import event.TimeStamp;

/**
 * Contains array of PQPoints which can be evaluated independently by
 * the market (i.e., the bid is divisible). Bids are contained in
 * an array sorted by price and quantity, both descending.
 * 
 * @author ewah
 */
public class PQBid extends Bid {

	public TreeSet<PQPoint> bidTreeSet;
	
	public PQBid(Agent agent, Market market, TimeStamp submissionTime) {
		super(agent, market, submissionTime);
		bidTreeSet = new TreeSet<PQPoint>(new PQPointComparator());
	}

	public PQBid(PQBid other) {
		super(other);
		// XXX Copied from AB3D. Potentially not a copy constructor, don't change.
		this.bidTreeSet = other.bidTreeSet;
	}
	
	/**
	 * Add point p to bidTreeSet
	 * 
	 * @param p PQPoint to add to the bid
	 */
	public void addPoint(PQPoint p) {
		bidTreeSet.add(p);
	}

	/**
	 * Add point specified by quantity and price to bidArray
	 * 
	 * @param q quantity
	 * @param p price
	 */
	public void addPoint(int q, Price p) {
		PQPoint pq = new PQPoint(q, p);
		pq.Parent = this;
		addPoint(pq);
	}

	/**
	 * Add multiple points.
	 * 
	 * @param points
	 */
	public void addPoints(PQPoint[] points) {
		for (int i = 0; i < points.length; i++) {
			addPoint(points[i]);
		}
	}
	
	/**
	 * @return price as String
	 */
	public String priceString() {
		return new Float(bidTreeSet.first().price.price).toString();
	}
	
	/**
	 * @return quantity as String
	 */
	public String quantityString() {
		return new Integer(bidTreeSet.first().quantity).toString();
	}
	
	/**
	 * Assumes divisible bids for now, so the other must be a single point.
	 * strict determines whether a tie is considered to be a winning bid
	 *
	 * @param minimum min bid at which to evaluate this bid
	 * @param strict  whether to compare based on price > or >=, 1=strict, 0 o/w
	 * @return number of units this is an offer for at a price of quote
	 */
	public Bid netOffer(Bid minimum, int strict) {
		PQBid netoffer = new PQBid(this);
		Price price = ((PQBid) minimum).bidTreeSet.first().getPrice();
		if (bidTreeSet == null || bidTreeSet.isEmpty()) return null;

		int quantity = 0;
		for (Iterator<PQPoint> i = bidTreeSet.iterator(); i.hasNext(); ) {
			PQPoint pq = i.next();
			int bidQ = pq.getQuantity();
			// determine quantity available at the minimum bid price
			if ((bidQ > 0 && pq.getPrice().compareTo(price) >= strict) ||
					(bidQ < 0 && pq.getPrice().compareTo(price) <= -1* strict)) {
				quantity += pq.getQuantity();
			}
		}
		netoffer.addPoint(quantity, new Price(0));
		return netoffer;
	}

	/**
	 * compute sell dominance
	 * dominance is an offer to buy/sell at least as much as the other bid at all prices and
	 * greater for at least one price
	 *
	 * @param otherBid other PQBid that this bid is compared to
	 * @return 1/0/-1 if this bid dominates/not comparable/dominated by other for quantites < 0
	 */
	public int sDominates(Bid otherBid) {

		PQBid other = (PQBid) otherBid;
		int thisP = -1;
		int otherP = -1;
		int thisNetAlloc = 0;
		int otherNetAlloc = 0;
		int dominates = 0;
		int dominated = 0;

		if (bidTreeSet == null || bidTreeSet.isEmpty()) return 0;

		PQPoint[] bidArray = bidTreeSet.toArray(new PQPoint[0]);
		PQPoint[] otherArray = other.bidTreeSet.toArray(new PQPoint[0]);

		//find the lowest sell offer  in this and other bid (bid is ordered & monotone)
		while ((thisP < bidTreeSet.size() - 1) && (bidArray[thisP + 1].getQuantity() < 0))
			thisP++;
		while ((otherP < other.bidTreeSet.size() - 1) && (otherArray[otherP + 1].getQuantity() < 0))
			otherP++;

		/* for each price point in the other bid, add up the quantity this bid offers
		 *  at a price >= other
		 *  algorithm ends up comparing at each price point of both bids
		 */
		PQPoint nextPoint;
		int i = thisP, j = otherP;
		while ((i >= 0) || (j >= 0)) {
			//compute the next price at which bids must be compared
			if (i < 0)
				nextPoint = otherArray[j];
			else if ((j < 0) || (bidArray[i].comparePrice(otherArray[j]) < 0))
				nextPoint = bidArray[i];
			else
				nextPoint = otherArray[j];

			//add up how much each bid offers at price <= nextprice
			while ((i >= 0) && (bidArray[i].comparePrice(nextPoint) <= 0)) {
				thisNetAlloc += bidArray[i--].getQuantity();
			}
			while ((j >= 0) && (otherArray[j].comparePrice(nextPoint) <= 0)) {
				otherNetAlloc += otherArray[j--].getQuantity();
			}
			//debug System.out.println("At the point "+nextPoint.toQPString()+
			// "this/other: "+thisNetAlloc+" "+otherNetAlloc);

			//compare for dominance at this price, quantities will be negative
			if (thisNetAlloc < otherNetAlloc)
				dominates = 1;
			if (otherNetAlloc < thisNetAlloc)
				dominated = 1;
		}
		return (dominates - dominated);
	}

	/**
	 * compute buy dominance
	 * domination is an offer for at least as many units at all prices, with greater
	 * offer for at least one price
	 *
	 * @param otherbid other PQBid that this bid is compared to
	 * @return 1/0/-1 if this bid  dominates/not comparable/dominated for quantites > 0
	 */
	public int bDominates(Bid otherbid) {

		PQBid other = (PQBid) otherbid;
		int thisP = 0;
		int otherP = 0;
		int thisNetAlloc = 0;
		int otherNetAlloc = 0;
		int dominates = 0;
		int dominated = 0;

		if (bidTreeSet == null || bidTreeSet.isEmpty()) return 0;

		PQPoint[] bidArray = bidTreeSet.toArray(new PQPoint[0]);
		PQPoint[] otherArray = other.bidTreeSet.toArray(new PQPoint[0]);
		
		//find the highest buy offer in this and other bid
		//(bids are ordered(descending) & monotone)
		if ((bidArray == null) && (other.containsBuyOffers()))
			return -1;
		else if ((otherArray == null) && containsBuyOffers())
			return 1;
		else if ((bidArray == null) || (otherArray == null))
			return 0;


		while ((thisP < bidArray.length) && (bidArray[thisP].getQuantity() < 0))
			thisP++;
		while ((otherP < otherArray.length) && (otherArray[otherP].getQuantity() < 0))
			otherP++;

		/* for each distinct price point P of bids' combined points,
		 *add up the quantity each bid offers  at a price >= P
		 */
		PQPoint nextPoint;
		int i = thisP, j = otherP;
		while ((i < bidArray.length) || (j < otherArray.length)) {

			//compute the next price at which bids must be compared
			if (i == bidArray.length)
				nextPoint = otherArray[j];
			else if ((j == otherArray.length) ||
					bidArray[i].comparePrice(otherArray[j]) > 0)
				nextPoint = bidArray[i];
			else
				nextPoint = otherArray[j];

			//add up how much each bid offers at price >= nextprice
			while ((i < bidArray.length) && (bidArray[i].comparePrice(nextPoint) >= 0)) {
				thisNetAlloc += bidArray[i++].getQuantity();
			}

			while ((j < otherArray.length) && (otherArray[j].comparePrice(nextPoint) >= 0)) {
				otherNetAlloc += otherArray[j++].getQuantity();
			}

			//System.out.println("At the point "+nextPoint.toQPString()+
			//"this/other: "+thisNetAlloc+":"+ otherNetAlloc);

			//compare for dominance at this price
			if (thisNetAlloc > otherNetAlloc)
				dominates = 1;
			if (otherNetAlloc > thisNetAlloc)
				dominated = 1;
		}
		return (dominates - dominated);
	}
	
	/**
	 * does bid contain offers to buy
	 *
	 * @return true if the PQBid contains offers to buy
	 */
	public boolean containsBuyOffers() {
		if (bidTreeSet == null || bidTreeSet.isEmpty()) return false;

		for (Iterator<PQPoint> i = bidTreeSet.iterator(); i.hasNext(); )
			if (i.next().getQuantity() > 0) return true;

		return false;
	}

	/**
	 * does bid contain offers to sell
	 *
	 * @return true if the PQBid contains offers to sell
	 */
	public boolean containsSellOffers() {
		if (bidTreeSet == null || bidTreeSet.isEmpty()) return false;

		for (Iterator<PQPoint> i = bidTreeSet.iterator(); i.hasNext(); )
			if (i.next().getQuantity() < 0) return true;

		return false;
	}
	
	/**
	 * similar to bDominates & sDominates, but without strict domination
	 *
	 * @param otherBid
	 * @return true if this bid will sell & buy at least as much as other at all prices
	 */
	public boolean contains(Bid otherBid) {
		if (otherBid == null) return true;

		PQBid other = (PQBid) otherBid;
		
		PQPoint[] bidArray = bidTreeSet.toArray(new PQPoint[0]);
		PQPoint[] otherArray = other.bidTreeSet.toArray(new PQPoint[0]);
		
		
		if (otherArray == null) return true;
		if (bidArray == null) return false;
		int otherQ;
		int i = 0, j = 0;
		int thisQ = 0;
		Price thisP = null;
		
		// Go through sell points in the other bid
		while ((i < otherArray.length) &&
				(otherQ = otherArray[i].getQuantity()) < 0) {

			Price otherP = otherArray[i++].getPrice();
			while (otherQ != 0) {

				//grab the next PQ from this bid if we're exhausted
				if (thisQ == 0) {
					if (j >= bidArray.length)
						return false;
					PQPoint PQ = bidArray[j++];
					thisP = PQ.getPrice();
					thisQ = PQ.getQuantity();
				}
				// if this is a buy point, we can't contain the other bid
				if (thisQ > 0)
					return false;

				//if this point is higher price (weaker sell), get next point
				if (thisP.compareTo(otherP) > 0)
					thisQ = 0;

				int trans = Math.max(otherQ, thisQ);
				thisQ -= trans;
				otherQ -= trans;
			}
		}
		
		//we've now exhausted all sell points in the other bid
		//find the first buy point in this bid
		while ((j < bidArray.length) && (bidArray[j].getQuantity() < 0)) j++;

		//iterate over all buy points in other bid
		//try to exhaust with points in this bid
		thisQ = 0;
		thisP = null;
		while (i < otherArray.length) {
			otherQ = otherArray[i].getQuantity();
			Price otherP = otherArray[i++].getPrice();
			while (otherQ != 0) {
				if (thisQ == 0) {
					if (j >= bidArray.length)
						return false;
					PQPoint PQ = bidArray[j++];
					thisP = PQ.getPrice();
					thisQ = PQ.getQuantity();
				}
				//if this sell point isn't strong enough, we can't use it
				if ((thisP.compareTo(otherP) > 0))
					thisQ = 0;
				int trans = Math.min(otherQ, thisQ);
				thisQ -= trans;
				otherQ -= trans;
			}
		}
		// we've now exhausted the entire other bid with this bid,
		//so containment is true
		return true;
	}
}
