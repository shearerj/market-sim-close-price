package entity.agent;

import java.util.Collection;
import java.util.Random;

import utils.Rands;

/**
 * Idea from: Tesauro & Das, "High-Performance Bidding Agents for the Continuous
 * Double Auction," EC-01.
 * 
 * When agents can trade multiple units, we use an array of profit margins
 * of the size of the number of units. Different units have different
 * limit prices, so they require different profit margins to trade at
 * equilibrium. There is one margin per traded unit.
 * 
 * In the paper, the margins are not statistically independent; the limit prices
 * of the less valuable units influence the initial margins of the more
 * valuable units---how? TODO margins independent? 
 * 
 * Margins: <code>getValue</code> is based on current (or projected) position
 * balance. <code>setValue</code> is similar.
 * 
 * NOTE: Margins only work with single quantity changes.
 * 
 * @author ewah
 *
 */
class Margin extends QuantityIndexedValue<Double> {
	
	private static final long serialVersionUID = -3749423779545857329L;

	public Margin() {
		super(0, 0.0);
	}
	
	/**
	 * @param maxPosition
	 * @param rand
	 * @param a
	 * @param b
	 */
	public Margin(int maxPosition, Random rand, double a, double b) {
		super(maxPosition, 0.0);

		double[] values = new double[maxPosition * 2];
		for (int i = 0; i < values.length; i++)
			values[i] = Rands.nextUniform(rand, a, b) *	(i >= maxPosition ? -1 : 1);
		// margins for buy orders are negative

		for (double value : values)
			this.values.add(new Double(value));
	}

	/**
	 * Protected constructor for testing purposes.
	 * 
	 * @param maxPosition
	 * @param values
	 */
	protected Margin(int maxPosition, Collection<Double> values) {
		super(maxPosition, 0.0, values);
	}

}
