/** 
 * $Id: PQPoint.java,v 1.16 2004/05/12 22:31:41 lschvart Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 */
package market;

import java.util.*;

import entity.Agent;
import entity.Market;

/**
 * a single price-quantity bid point
 */
public class PQPoint extends Point implements Comparable<PQPoint> {
	
	protected Price price;
	protected int quantity;
	protected int originalQuantity;

	public PQBid Parent; 	//refer back to enclosing bid for timestamp/agentID
	static Comparator<PQPoint> comp;
	public static Comparator<PQPoint> compQtyPrice = new PQPointComparator(1, 2, 0);

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
		if (comp == null) comp = new PQPointComparator(); 
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
	 * @param P Parent PQOrder
	 */
	public PQPoint(int q, Price p, PQBid P)
	{
		if (comp == null) comp = new PQPointComparator(); 
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
	 * split this PQPoint into two, with the new PQPoint having 
	 * quantityToShare, this one having whatever is left over
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
	 * get the agentID of the parent PQOrder
	 *
	 * @return the agentID of the parent Bid
	 */
	public int getAgentID()
	{
		if (Parent == null) return 0;
		else return Parent.getAgent().getID();
	}
	
	public Agent getAgent() {
		return Parent.getAgent();
	}
	
	public Market getMarket() {
		return Parent.getMarket();
	}

	/**
	 * decrement the quantity left in the PQPoint
	 *
	 *@param q quantity transacted
	 */
	public void transact(int q)
	{
		quantity -= q;
	}
	
	/**
	 * set the price of the point
	 *
	 * @param p price
	 */
	public void setPrice(Price p) 
	{
		price = p;
	}
	
	/**
	 * get the price of the point
	 *
	 * @return the price
	 */
	public Price getPrice()
	{
		return price;
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
	public void setQuantity(int q)
	{
		quantity = q;
	}
	
	/**
	 * get the current quantity of the point
	 *
	 * @return current quantity
	 */
	public int getQuantity()
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
	public String toString()
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
		if (P1.Parent.submitTime.compareTo(P2.Parent.submitTime) < 0)
			return P1.getPrice();
		else
			return P2.getPrice();
	}
	
	/**
	 * compare points based on price
	 *
	 * @param other point to compare to
	 * @return -1/0/1 if this price is <=> other
	 */
	public int comparePrice(PQPoint other)
	{
		return this.getPrice().compareTo(other.getPrice());
	}
	
	/**
	 * compare pqp's based on default comparator (dec price, inc time, dec q)
	 * @param other bid to compare to
	 * @return  -1/0/1
	 */
	public int compareTo(PQPoint other) 
	{
		return comp.compare(this, other);
	}
}




