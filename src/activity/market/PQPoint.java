/** 
 * $Id: PQPoint.java,v 1.16 2004/05/12 22:31:41 lschvart Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 *  
 *  Edited 2012/06/08 by ewah
 */

package activity.market;

import java.util.*;


/**
 * a single price-quantity bid point
 * 
 * TODO - refactor names of functions to java capitalization
 */
public class PQPoint implements Comparable {
	
	protected Price price;
	protected int quantity;
	protected int originalQuantity;

	public PQBid Parent; 	//refer back to enclosing bid for timestamp/agentID
	static Comparator Comp;
	public static Comparator compQtyPrice = new PQP_Comp(1, 2, 0);

	/**
	 * create point with zero price/quantity
	 */
	public PQPoint()
	{
		price = null;
		quantity = 0;
		originalQuantity = 0;
		Parent = null;
	}

	/**
	 * Create point with given price/quantity.
	 * @param q quantity
	 * @param p price
	 */
	public PQPoint(int q, Price p)
	{
		if (Comp == null) Comp = new PQP_Comp(); 
		price = p; 
		quantity = q; 
		Parent = null;
		originalQuantity = q;
	}

	/**
	 * construct a new PQPoint with P being the parent bid holding this point
	 *
	 * @param q quantity
	 * @param p price
	 * @param P Parent PQBid
	 */
	public PQPoint(int q, Price p , PQBid P)
	{
		if (Comp == null) Comp = new PQP_Comp(); 
		price = p; 
		quantity = q; 
		Parent = P;
		originalQuantity = q;

	}
	/**
	 * create a copy of the other point
	 *
	 * @param other point to copy
	 */
	public PQPoint(PQPoint other)
	{
		price = other.price;
		quantity = other.quantity;
		Parent = other.Parent;
		originalQuantity = other.originalQuantity;
	}
	
	/**
	 * split this PQPoint into two,
	 * with the new PQPoint having quantityToShare, this one having
	 * whatever is left over
	 * (necessary for FourHeap algorithms)
	 *
	 * @param quantityToShare is the amount to remove from this PQPoint
	 * @return PQPoint with q = quantityToShare
	 */
	public PQPoint split(int quantityToShare)
	{
		//assert (quantityToShare < quantity) : "invalid PQPoint split";

		quantity = quantity - quantityToShare;
		PQPoint sibling = new PQPoint(quantityToShare,price,Parent);
		if (Parent != null) Parent.addPoint(sibling);
		return sibling;
	}

	/**
	 * get the agentID of the parent PQBid
	 *
	 * @return the agentID of the parent Bid
	 */
	public  int getAgentID()
	{
		if (Parent == null) return 0;
		else return Parent.agentID.intValue();
	}

	/**
	 * decrement the quantity left in the PQPoint
	 *
	 *@param q quantity transacted
	 */
	public void transact(int q)
	{
		/*assert( ((q > 0) && (quantity > 0)) ||
	    ((q < 0) && (quantity < 0))) :
      "pqp::transact treating a sell as a buy PQPoint";
    assert (Math.abs(q) <= Math.abs(quantity)) :
      "pqp::transact transaction size exceeds size of PQPoint";
		 */
		quantity -= q;
	}
	/**
	 * set the price of the point
	 *
	 * @param p price
	 */
	public void setprice(Price p) 
	{
		price = p;
	}
	/**
	 * get the price of the point
	 *
	 * @return the price
	 */
	public Price getprice()
	{
		return  price;
	}
	/**
	 * what was the original quantity this point held
	 *
	 * @return original quantity
	 */
	public int originalQuantity()
	{
		return originalQuantity;
	}
	/**
	 * set the quantity of the point
	 *
	 * @param q quantity
	 */
	public void setquantity(int q)
	{
		quantity = q;
	}
	
	/**
	 * get the current quantity of the point
	 *
	 * @return current quantity
	 */
	public int getquantity()
	{
		return quantity;
	}
	
	/**
	 * print p/q to stdout
	 */
	public void print()
	{
		System.out.println(price + " " + quantity);
	}
	
	/**
	 * generate a string representation of the point
	 *
	 * @return a String in the form (Q P) useful for generating bidstring
	 */
	public String toQPString()
	{
		String S = new String("(");
		S = S + quantity + " " + price.toString() + ")";
		return S;
	}

	/**
	 * what is the price of the PQPoint with the earlier timestamp
	 *
	 * @param P1 point 1
	 * @param P2 point 2
	 * @return the price of the PQPoint with the earlier timestamp
	 */
	static Price earliestPrice(PQPoint P1, PQPoint P2)
	{
		if (P1.Parent.timestamp.compareTo(P2.Parent.timestamp) < 0)
			return P1.getprice();
		else
			return P2.getprice();
	}
	/**
	 * compare points based on price
	 *
	 * @param other point to compare to
	 * @return -1/0/1 if this price is <=> other
	 */
	public int comparePrice(PQPoint other)
	{
		return this.getprice().compareTo(other.getprice());
	}
	/**
	 * compare pqp's based on default comparator (dec price, inc time, dec q)
	 * @param other bid to compare to
	 * @return  -1/0/1
	 */
	public int compareTo(Object other) 
	{
		PQPoint other_pq = (PQPoint)other;

		return Comp.compare(this,other_pq);

	}

	/**
	 * Comparator class for PQPoints
	 * <ul>
	 * <li>Options are to sort on price/quantity/timestamp, in any order
	 * and in any order of importance.  
	 * <li>The constructor takes an integer for each of Price, Quantity, timestamp,
	 * the sign of each arguments indicates whether to sort that field in
	 * ascending (+) descending (-) or non-ordered (0), and the relative
	 * magnitudes of the three fields designate the order of importance of each,
	 * where highest magnitude (abs value) will be the first fields on which to sort.
	 * <li> eg:  parg = 3, qarg = 2, targ = -1 will sort
	 * first in ascending price, breaking ties in ascending quantity, breaking
	 * remaining ties in descending timestamps.
	 * </ul>
	 */
	public static class PQP_Comp implements Comparator
	{
		private int pOrder; //ascending/descending
		private int qOrder;
		private int tOrder;

		/**
		 *  the relative magnitudes of the three arguments determines the
		 * order in which the sorts will be performed, where highest
		 * magnitude takes precedence for the comparison.
		 *
		 * @param pAscending  > 0 sorts in ascending price, < 0 descending
		 * @param qAscending > 0 sorts in ascending quantity, <0 descending
		 * @param tAscending > 0 sorts in ascending timestamp . . .

		 */
		public PQP_Comp(int pAscending, int qAscending, int tAscending)
		{
			pOrder =  pAscending;
			qOrder =  qAscending;
			tOrder =  tAscending;
		}

		/** default is to sort in descending order of price/ inc date, dec quantity
		 */
		public PQP_Comp()
		{ 
			pOrder = -3;
			qOrder = -1;
			tOrder = 2;
		}

		/**
		 * compare to points based on default comparator
		 *
		 * @param o1 object 1
		 * @param o2 object to compare to,  
		 * @return 1,0,-1 if o1 is >=< o2
		 */
		public int compare(Object o1, Object o2)
		{
			PQPoint pq1 = (PQPoint)o1;
			PQPoint pq2 = (PQPoint)o2;
			int P,Q,T,O;

			//compare on price
			P = pq1.getprice().compareTo(pq2.getprice());

			//compare on quantity
			Q = (new Integer(pq1.getquantity())).compareTo(new Integer(pq2.getquantity()));

			if ((pq1.Parent != null) && (pq1.Parent.timestamp != null) &&
					(pq2.Parent != null) && (pq2.Parent.timestamp != null))
			{
				T = (pq1.Parent.timestamp.compareTo(pq2.Parent.timestamp));

				if (T == 0)
					T = pq1.Parent.bidID.compareTo(pq2.Parent.bidID);
			}
			else
			{
				T = 0;
			}

			//use the hashcodes as a last resort
			O = new Integer((o1.hashCode())).compareTo(new Integer(o2.hashCode()));

			//this will be <=> 0 based on comparisons and rankings of fields
			int ret = 2*(pOrder*P+qOrder*Q+tOrder*T) + O ;

			return (ret);
		}
	}
}




