/**
 *  $Id: PQBid.java,v 1.37 2004/06/03 17:50:56 lschvart Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 *  
 *  2012/06/13 edited by ewah (remove all Point refs, change to PQPoint)
 */
package activity.market;

import java.util.*;

/**
 * PQBid Class
 * contains a set of PQPoints which can be evaluated independently by
 * the auction (i.e. the bid is divisible). Bids are contained in an 
 * array sorted by Price & Quantity, both descending.
 */

public class PQBid extends Bid {
	
	//PQPoints are sorted in descending order of price/quantity
	public PQPoint[] bidArray;

	/**
	 * create PQBid with null bidArray
	 */
	public PQBid() {
		super();
	}

	/**
	 * set the bidArray to null
	 */
	public void clearPoints() {
		bidArray = null;
	}

	/**
	 * add the point p, re-sort the bidArray
	 *
	 * @param p PQPoint to add to this bid
	 */
	public void addPoint(PQPoint p) {  // changed to PQPoint input rather than Point
		int oldlength = 0;
		PQPoint[] oldbidArray = bidArray;
		if (bidArray != null)
			oldlength = oldbidArray.length;
		bidArray = new PQPoint[oldlength + 1];
		int i = 0;
		while (i < oldlength && oldbidArray[i].compareTo(p) < 0) {
			bidArray[i] = oldbidArray[i++];
		}
		bidArray[i++] = p;
		while (i < oldlength + 1) {
			bidArray[i] = oldbidArray[i - 1];
			i++;
		}
	}

	/**
	 * adds the points and sorts the bidArray
	 *
	 * @param q quantity of point to add
	 * @param p price of point to add
	 */
	public void addPoint(int q, Price p) {
		PQPoint pq = new PQPoint(q, p);
		addPoint(pq);
	}


	/**
	 * assumes divisible bids for now, so the other must be a single point
	 * strict determines whether a tie is considered to be a winning bid
	 *
	 * @param minimum min bid at which to evaluate this bid
	 * @param strict  whether to compare based on price > or >=, 1=strict
	 * @return number of units this is an offer for at a price of quote
	 */
	public Bid netOffer(Bid minimum, int strict) {
		PQBid netoffer = new PQBid();
		Price price = ((PQBid) minimum).bidArray[0].getprice();
		if (bidArray == null) return null;

		int quantity = 0;
		for (int i = 0; i < bidArray.length; i++) {

			PQPoint PQ = bidArray[i];
			int bidQ = PQ.getquantity();
			if (((bidQ > 0) && (PQ.getprice().compareTo(price) >= strict)) ||
					((bidQ < 0) && (PQ.getprice().compareTo(price) <= -1 * strict))) {
				quantity += PQ.getquantity();
			}
		}
		netoffer.addPoint(quantity, new Price(0));
		return netoffer;
	}

	/**
	 * similar to bDominates & sDominates, but without strict domination
	 *
	 * @param other
	 * @return true if this bid will sell & buy at least as much as other at all prices
	 */
	public boolean contains(Bid other) {
		if (other == null) return true;

		PQBid otherPQ = (PQBid) other;
		if (otherPQ.bidArray == null) return true;
		if (bidArray == null) return false;
		int otherQ;
		int i = 0, j = 0;
		int thisQ = 0;
		Price thisP = null;
		while ((i < otherPQ.bidArray.length) &&
				(otherQ = otherPQ.bidArray[i].getquantity()) < 0) {

			Price otherP = otherPQ.bidArray[i++].getprice();
			while (otherQ != 0) {

				//grab the next PQ from this bid if we're exhausted
				if (thisQ == 0) {
					if (j >= bidArray.length)
						return false;
					PQPoint PQ = bidArray[j++];
					thisP = PQ.getprice();
					thisQ = PQ.getquantity();
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
		while ((j < bidArray.length) && (bidArray[j].getquantity() < 0)) j++;

		//iterate over all buy points in other bid
		//try to exhaust with points in this bid
		thisQ = 0;
		thisP = null;
		while (i < otherPQ.bidArray.length) {
			otherQ = otherPQ.bidArray[i].getquantity();
			Price otherP = otherPQ.bidArray[i++].getprice();
			while (otherQ != 0) {
				if (thisQ == 0) {
					if (j >= bidArray.length)
						return false;
					PQPoint PQ = bidArray[j++];
					thisP = PQ.getprice();
					thisQ = PQ.getquantity();
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

	/**
	 * compute sell dominance
	 * dominance is an offer to buy/sell at least as much as the other bid at all prices and
	 * greater for at least one price
	 *
	 * @param otherbid other PQBid that this bid is compared to
	 * @return 1/0/-1 if this bid dominates/not comparable/dominated by other for quantites < 0
	 */
	public int sDominates(Bid otherbid) {
		PQBid other = (PQBid) otherbid;
		int thisP = -1;
		int otherP = -1;
		int thisNetAlloc = 0;
		int otherNetAlloc = 0;
		int dominates = 0;
		int dominated = 0;

		if (bidArray == null) return 0;

		//find the lowest sell offer  in this and other bid (bid is ordered & monotone)
		while ((thisP < bidArray.length - 1) && (bidArray[thisP + 1].getquantity() < 0))
			thisP++;
		while ((otherP < other.bidArray.length - 1) && (other.bidArray[otherP + 1].getquantity() < 0))
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
				nextPoint = other.bidArray[j];
			else if ((j < 0) || (bidArray[i].comparePrice(other.bidArray[j]) < 0))
				nextPoint = bidArray[i];
			else
				nextPoint = other.bidArray[j];

			//add up how much each bid offers at price <= nextprice
			while ((i >= 0) && (bidArray[i].comparePrice(nextPoint) <= 0)) {
				thisNetAlloc += bidArray[i--].getquantity();
			}
			while ((j >= 0) && (other.bidArray[j].comparePrice(nextPoint) <= 0)) {
				otherNetAlloc += other.bidArray[j--].getquantity();
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

		//find the highest buy offer in this and other bid
		//(bids are ordered(descending) & monotone)
		if ((bidArray == null) && (other.containsBuyOffers()))
			return -1;
		else if ((other.bidArray == null) && containsBuyOffers())
			return 1;
		else if ((bidArray == null) || (other.bidArray == null))
			return 0;


		while ((thisP < bidArray.length) && (bidArray[thisP].getquantity() < 0))
			thisP++;
		while ((otherP < other.bidArray.length) && (other.bidArray[otherP].getquantity() < 0))
			otherP++;

		/* for each distinct price point P of bids' combined points,
		 *add up the quantity each bid offers  at a price >= P
		 */
		PQPoint nextPoint;
		int i = thisP, j = otherP;
		while ((i < bidArray.length) || (j < other.bidArray.length)) {

			//compute the next price at which bids must be compared
			if (i == bidArray.length)
				nextPoint = other.bidArray[j];
			else if ((j == other.bidArray.length) ||
					bidArray[i].comparePrice(other.bidArray[j]) > 0)
				nextPoint = bidArray[i];
			else
				nextPoint = other.bidArray[j];

			//add up how much each bid offers at price >= nextprice
			while ((i < bidArray.length) && (bidArray[i].comparePrice(nextPoint) >= 0)) {
				thisNetAlloc += bidArray[i++].getquantity();
			}

			while ((j < other.bidArray.length) && (other.bidArray[j].comparePrice(nextPoint) >= 0)) {
				otherNetAlloc += other.bidArray[j++].getquantity();
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
		if (bidArray == null) return false;

		for (int i = 0; i < bidArray.length; i++)
			if (bidArray[i].getquantity() > 0)
				return true;

		return false;
	}

	/**
	 * does bid contain offers to sell
	 *
	 * @return true if the PQBid contains offers to sell
	 */
	public boolean containsSellOffers() {
		if (bidArray == null) return false;

		for (int i = 0; i < bidArray.length; i++)
			if (bidArray[i].getquantity() < 0)
				return (true);

		return false;
	}


	public boolean selfTransacts() {
		float lowestSell = Float.MAX_VALUE;
		float highestBuy = 0;
		for (int i = 0; i < bidArray.length; i++) {
			PQPoint p = bidArray[i];
			if (p.getquantity() > 0 && p.getprice().floatValue() > highestBuy)
				highestBuy = p.getprice().floatValue();
			else if (p.getquantity() < 0 && p.getprice().floatValue() < lowestSell)
				lowestSell = p.getprice().floatValue();
		}
		if (lowestSell > highestBuy)
			return false;
		else
			return true;

	}

	/**
	 * @return the price of the first unit of this bid (assumes there is only one point)
	 */
	public String quoteString() {
		return new Float(bidArray[0].price.price).toString();
	}

	public String allocString() {
		return new Integer(bidArray[0].quantity).toString();
	}


	/**
	 * compare bidArray of this bid versus bidArray of other on prices and quantities only
	 * points with quantity == 0 are excluded
	 */
	public String sameBidArray(Bid other) {

		Hashtable tmp1 = new Hashtable();
		Hashtable tmp2 = new Hashtable();

		// eliminate points with qty == 0
		int m1 = (this.bidArray != null) ? this.bidArray.length : 0;
		for (int i = 0; i < m1; i++) {
			if (this.bidArray[i].getquantity() != 0) {
				Integer prev = (Integer) tmp1.get(this.bidArray[i].getprice().toDouble());
				if (prev == null) prev = new Integer(0);
				tmp1.put(this.bidArray[i].getprice().toDouble(), new Integer(prev.intValue() + this.bidArray[i].getquantity()));
			}
		}

		int m2 = (other != null && ((PQBid) other).bidArray != null) ? ((PQBid) other).bidArray.length : 0;
		for (int i = 0; i < m2; i++) {
			if (((PQBid) other).bidArray[i].getquantity() != 0) {
				Integer prev = (Integer) tmp2.get(((PQBid) other).bidArray[i].getprice().toDouble());
				if (prev == null) prev = new Integer(0);
				tmp2.put(((PQBid) other).bidArray[i].getprice().toDouble(), new Integer(prev.intValue() + ((PQBid) other).bidArray[i].getquantity()));
			}
		}

		if (tmp1.size() != tmp2.size())
			return "size " + tmp1.size() + " != " + tmp2.size();

		for (Iterator i = tmp1.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			Double p1 = (Double) e.getKey();
			Integer q1 = (Integer) tmp1.get(p1);
			Integer q2 = (Integer) tmp2.get(p1);

			if (q1 == null) return "ERROR";
			if (q2 == null) return "price " + p1 + " is in this but not in other";

			if (q1.compareTo(q2) != 0) return "price " + p1 + " has qty " + q1 + " != " + q2;
		}
		return null;
	}


	public void addPoints(PQPoint[] points) {
		for (int i = 0; i < points.length; i++) {
			addPoint(points[i]);
		}
	}

	public PQPoint[] getBidArray() {
		return bidArray;
	}

	/**
	 * get xorpoint with maximum price
	 * buyOrSell > 0 for buy points
	 * buyOrSell < 0 for sell points
	 */
	public PQPoint getMaxPricePoint(int buyOrSell) {
		PQPoint pt = new PQPoint(0, new Price(Float.MIN_VALUE));
		for (int i = 0; i < bidArray.length; i++) {
			if ((buyOrSell > 0 && bidArray[i].getquantity() > 0) || (buyOrSell < 0 && bidArray[i].getquantity() < 0)) {
				if (bidArray[i].getprice().getPrice() > pt.getprice().getPrice()) {
					pt = new PQPoint(bidArray[i].getquantity(), bidArray[i].getprice());
				}
			}
		}
		return ((pt.getprice().getPrice() == Float.MIN_VALUE) ? null : pt);
	}


	/**
	 * get xorpoint with minimum price
	 * buyOrSell > 0 for buy points
	 * buyOrSell < 0 for sell points
	 */
	public PQPoint getMinPricePoint(int buyOrSell) {
		PQPoint pt = new PQPoint(0, new Price(Float.MAX_VALUE));
		for (int i = 0; i < bidArray.length; i++) {
			if ((buyOrSell > 0 && bidArray[i].getquantity() > 0) || (buyOrSell < 0 && bidArray[i].getquantity() < 0)) {
				if (bidArray[i].getprice().getPrice() < pt.getprice().getPrice()) {
					pt = new PQPoint(bidArray[i].getquantity(), bidArray[i].getprice());
				}
			}
		}
		return ((pt.getprice().getPrice() == Float.MAX_VALUE) ? null : pt);
	}

}

