package market;

import java.util.Comparator;

import entity.Agent;
import entity.Market;

public class Point implements Comparable<Point> {

	// FIXME This class sucks
	protected Agent agent;
	protected Price price;
	protected int quantity;
	protected int originalQuantity;

	public Bid Parent; // refer back to enclosing bid for timestamp/agentID
	static Comparator<Point> comp;
	public static Comparator<Point> compQtyPrice = new PointComparator(1,
			2, 0);

	/**
	 * create point with zero price/quantity
	 */
	public Point() {
		price = null;
		quantity = 0;
		originalQuantity = 0;
		Parent = null;
	}

	/**
	 * Create point with given price/quantity.
	 * 
	 * @param q
	 *            quantity
	 * @param p
	 *            price
	 */
	public Point(int q, Price p) {
		if (comp == null) comp = new PointComparator();
		price = p;
		quantity = q;
		Parent = null;
		originalQuantity = q;
	}

	/**
	 * construct a new Point with P being the parent bid holding this point
	 * 
	 * @param q
	 *            quantity
	 * @param p
	 *            price
	 * @param P
	 *            Parent PQOrder
	 */
	public Point(int q, Price p, Bid P) {
		if (comp == null) comp = new PointComparator();
		price = p;
		quantity = q;
		originalQuantity = q;
		Parent = P;
	}

	/**
	 * create a copy of the other point
	 * 
	 * @param other
	 *            point to copy
	 */
	public Point(Point other) {
		price = other.price;
		quantity = other.quantity;
		Parent = other.Parent;
		originalQuantity = other.originalQuantity;
	}

	/**
	 * split this Point into two, with the new Point having quantityToShare, this one having
	 * whatever is left over (necessary for FourHeap algorithms)
	 * 
	 * @param quantityToShare
	 *            is the amount to remove from this Point
	 * @return Point with q = quantityToShare
	 */
	public Point split(int quantityToShare) {
		// assert (quantityToShare < quantity) : "invalid Point split";

		quantity = quantity - quantityToShare;
		Point sibling = new Point(quantityToShare, price, Parent);
		if (Parent != null) Parent.addPoint(sibling);
		return sibling;
	}

	public Agent getAgent() {
		return Parent.getAgent();
	}

	public Market getMarket() {
		return Parent.getMarket();
	}

	/**
	 * decrement the quantity left in the Point
	 * 
	 * @param q
	 *            quantity transacted
	 */
	public void transact(int q) {
		quantity -= q;
	}

	/**
	 * set the price of the point
	 * 
	 * @param p
	 *            price
	 */
	public void setPrice(Price p) {
		price = p;
	}

	/**
	 * get the price of the point
	 * 
	 * @return the price
	 */
	public Price getPrice() {
		return price;
	}

	/**
	 * what was the original quantity this point held
	 * 
	 * @return original quantity
	 */
	public int originalQuantity() {
		return originalQuantity;
	}

	/**
	 * set the quantity of the point
	 * 
	 * @param q
	 *            quantity
	 */
	public void setQuantity(int q) {
		quantity = q;
	}

	/**
	 * get the current quantity of the point
	 * 
	 * @return current quantity
	 */
	public int getQuantity() {
		return quantity;
	}

	/**
	 * print p/q to stdout
	 */
	public void print() {
		System.out.println(price + " " + quantity);
	}

	/**
	 * what is the price of the Point with the earlier timestamp
	 * 
	 * @param P1
	 *            point 1
	 * @param P2
	 *            point 2
	 * @return the price of the Point with the earlier timestamp
	 */
	static Price earliestPrice(Point P1, Point P2) {
		if (P1.Parent.submitTime.compareTo(P2.Parent.submitTime) < 0)
			return P1.getPrice();
		else
			return P2.getPrice();
	}

	/**
	 * compare points based on price
	 * 
	 * @param other
	 *            point to compare to
	 * @return -1/0/1 if this price is <=> other
	 */
	public int comparePrice(Point other) {
		return this.getPrice().compareTo(other.getPrice());
	}

	/**
	 * compare pqp's based on default comparator (dec price, inc time, dec q)
	 * 
	 * @param other
	 *            bid to compare to
	 * @return -1/0/1
	 */
	public int compareTo(Point other) {
		return comp.compare(this, other);
	}

	/**
	 * @return a String in the form (Q P) useful for generating bidstring
	 */
	public String toString() {
		return "(" + quantity + " " + price + ")";
	}
	
}
