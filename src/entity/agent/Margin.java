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
 * valuable units---TODO how? or perhaps margins should be independent? 
 * 
 * Margins: <code>getValue</code> is based on current (or projected) position
 * balance. <code>setValue</code> is similar.
 * 
 * NOTE: Margins only work with single quantity changes.
 * 
 * @author ewah
 *
 */
class Margin implements Serializable, QuantityIndexedArray<Double> {

	private static final long serialVersionUID = -3749423779545857329L;
	
	protected final int offset;
	protected List<Double> values;

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

	/**
	 * @param currentPosition
	 * @param type
	 * @param value
	 */
	public void setValue(int currentPosition, OrderType type,
			double value) {
		switch (type) {
		case BUY:
			if (currentPosition + offset <= values.size() - 1 &&
					currentPosition + offset >= 0)
				values.set(currentPosition + offset, value);
			break;
		case SELL:
			if (currentPosition + offset - 1 <= values.size() - 1 && 
					currentPosition + offset - 1 >= 0)
				values.set(currentPosition + offset - 1, value);
			break;
		}
	}
	
	@Override
	public Double getValueFromQuantity(int currentPosition, int quantity,
			OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		
		// TODO need to implement for multiple units
		return new Double(0);
	}
}
