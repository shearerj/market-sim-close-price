package market;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * PRIVATEVALUE
 * 
 * Stores Prices in a vector of private values, sorted in descending order.
 * 
 * If (A, B) is the vector of private values, a trader with initial
 * net position = 0 has private valuation of A if selling 1 unit, and 
 * private value of B if buying 1 unit.
 * 
 * For a private valuation vector for maximum position q:
 * 	  ( a(-q),  a(-q+1), ..., a(-1), a(1), ..., a(q-1), a(q) )
 * Going from quantity -q to -q+1 is associated with value a(-q).
 * Going from quantity q to q-1 is associated with value a(q).
 * 
 * Private values are associated with changes in position. For example,
 * a trader with current balance of 1 who intends to buy 1 additional unit 
 * of the good will have private value a(2) for the additional unit.
 * 
 * As such, the getValueAt method in the Agent class is necessary to 
 * correctly determine the private value for a new order (as it is
 * based on both current position and the limit order quantity).
 * 
 * @author ewah
 */
public class PrivateValue {

	private ArrayList<Price> values;
	private ArrayList<Integer> quantities;
	
	public PrivateValue() {
		values = new ArrayList<Price>();
		quantities = new ArrayList<Integer>();
	}
	
	public PrivateValue(int[] alphas) {
		values = new ArrayList<Price>();
		for (int i = 0; i < alphas.length; i++) {
			values.add(new Price(alphas[i]));
		}
		// sort in descending order
        Collections.sort(values, Collections.reverseOrder());
		quantities = new ArrayList<Integer>();
		createQuantities(alphas.length);
	}
	
	public PrivateValue(ArrayList<Integer> alphas) {
		values = new ArrayList<Price>();
		for (int i = 0; i < alphas.size(); i++) {
			values.add(new Price(alphas.get(i)));
		}
		// sort in descending order
        Collections.sort(values, Collections.reverseOrder());
		quantities = new ArrayList<Integer>();
		createQuantities(alphas.size());
	}
	
	public PrivateValue(int alpha1, int alpha2) {
		values = new ArrayList<Price>();
		values.add(new Price(alpha1));
		values.add(new Price(alpha2));
		// sort in descending order
		Collections.sort(values, Collections.reverseOrder());
		quantities = new ArrayList<Integer>();
		createQuantities(-1, 1);
	}
	
	public PrivateValue(Price alpha1, Price alpha2) {
		values = new ArrayList<Price>();
		values.add(alpha1);
		values.add(alpha2);
		// sort in descending order
		Collections.sort(values, Collections.reverseOrder());
		quantities = new ArrayList<Integer>();
		createQuantities(-1, 1);
	}
	
	/**
	 * Given a quantity to buy or sell (+/-), return the associated private
	 * value.
	 * 
	 * @param q		quantity to buy or sell
	 * @return
	 */
	public Price getValueFromQuantity(int q) {
		if (quantities.contains(q))
			return values.get(quantities.indexOf(q));
		else
			return new Price(0);
	}
	
	/**
	 * Reverses the alpha values.
	 */
	public void reverseValues() {
		Collections.reverse(values);
	}
	
	/**
	 * Creates vector of (-qSell, -qSell+1, ..., -1, 1, ..., qBuy-1, qBuy)
	 * 
	 * @param qBuy
	 * @param qSell
	 */
	private void createQuantities(int qBuy, int qSell) {
		// error checking 
		if (qSell > 0) qSell = -qSell;
		if (qBuy < 0) qBuy = -qBuy;
		
		for (int i = qSell; i <= qBuy; i++) {
			if (i != 0) quantities.add(i);
		}
	}
	
	/**
	 * Creates vector of (-qMax, ..., -1, 1, ..., qMax) given the length of the
	 * alpha vector (equal to 2*qMax).
	 * 
	 * @param legnth
	 */
	private void createQuantities(int length) {
		int qMax = length / 2;
		for (int i = -qMax; i <= qMax; i++) {
			if (i != 0) quantities.add(i);
		}
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return Arrays.asList(values).toString();
	}
}
