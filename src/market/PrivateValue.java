package market;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * PrivateValue
 * 
 * Stores Prices in a vector of sorted private values.
 * Also stores the transition points (buy/sell)
 * 
 * If (alpha, beta) is the vector of private values, this means that
 * a trader will have private value of alpha if selling 1 more unit, and 
 * private value of beta if buying 1 unit. So the corresponding quantities
 * vector is (-1, 1).
 * 
 * There is no alpha associated with quantity 0.
 * 
 * For multi-quantity private valuation vectors of max position q:
 * (a(-q),  a(-q+1), ..., a(-1), a(1), ..., a(q-1), a(n))
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
	 * @param q
	 * @return
	 */
	public Price getValueAt(int q) {
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
	 * Creates vector of (-qMax, ..., -1, 1, ..., qMax)
	 * 
	 * @param qMax
	 */
	private void createQuantities(int qMax) {
		// error checking 
		if (qMax < 0) qMax = -qMax;

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
