package market;

import java.util.Arrays;

import utils.RandPlus;
import static utils.MathUtils.bound;
import static market.Price.ZERO;

/**
 * PRIVATEVALUE
 * 
 * Encapsulation of an agent's private value. <code>getValueFromQuantity</code>
 * is the main method that should be used.
 * 
 * @author ewah
 */
public class PrivateValue {

	protected final int offset;
	protected final Price[] prices;

	/**
	 * Constructor for an agent without private value. This will return
	 * Price.ZERO for every private value query.
	 */
	public PrivateValue() {
		offset = 0;
		prices = new Price[] { ZERO };
	}

	/**
	 * Randomly generate private values for positional changes
	 * 
	 * @param maxPosition
	 *            the maximum position the agent can take. Beyond max position
	 *            the change in private value is zero.
	 * @param var
	 *            Gaussian variance to use during random private value
	 *            initialization.
	 * @param rand
	 *            The random number generator to use for random generation.
	 */
	public PrivateValue(int maxPosition, double var, RandPlus rand) {
		// Produces the exact same distribution as before
		offset = maxPosition;
		prices = new Price[maxPosition * 2 + 1];
		for (int i = 0; i < prices.length; i++)
			prices[i] = new Price((int) Math.round(rand.nextGaussian(0, var)));
		Arrays.sort(prices);

		Price median = prices[offset];
		for (int i = 0; i < prices.length; i++)
			prices[i] = prices[i].minus(median);
	}

	/**
	 * @param position
	 *            Agent's current position
	 * @param quantity
	 *            Number of goods Agent will acquire from current position
	 * @return The change resulting from modifying ones position by that amount.
	 */
	public Price getValueFromQuantity(int position, int quantity) {
		return getValueAtPosition(position + quantity).minus(
				getValueAtPosition(position));
	}

	/**
	 * Get a peudo private value for the agent from holding a specific position.
	 * This call is experimental and may be deprecated in the future.
	 * 
	 * @param position
	 *            The position of the Agent
	 * @return The pseudo private value of the Agent at the position. This is
	 *         additive so
	 *         <code> getValueAtPosition(newPos).minus(getValueAtPosition(currentPos)) </code>
	 *         represents the change in value for going from position currentPos
	 *         to position newPos.
	 */
	public Price getValueAtPosition(int position) {
		return prices[bound(position + offset, 0, prices.length - 1)];
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(prices) ^ offset;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PrivateValue))
			return false;
		PrivateValue other = (PrivateValue) obj;
		return other.offset == this.offset
				&& Arrays.equals(other.prices, this.prices);
	}

	@Override
	public String toString() {
		return Arrays.toString(prices);
	}

}
