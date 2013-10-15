package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static entity.market.Price.ZERO;
import static utils.MathUtils.bound;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import utils.Rands;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import entity.market.Price;

/**
 * PRIVATEVALUE
 * 
 * Encapsulation of an agent's private value. <code>getValueFromQuantity</code>
 * is the main method that should be used.
 * 
 * @author ewah
 */
public class PrivateValue implements Serializable {

	private static final long serialVersionUID = -348702049295080442L;
	
	protected final int offset;
	protected final List<Price> prices;

	/**
	 * Constructor for an agent without private value. This will return
	 * Price.ZERO for every private value query.
	 */
	public PrivateValue() {
		offset = 0;
		prices = Collections.singletonList(ZERO);
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
	public PrivateValue(int maxPosition, double var, Random rand) {
		checkArgument(maxPosition > 0, "Max Position must be positive");
		
		// Identical to legacy generation in final output
		this.offset = maxPosition;
		double[] prices = new double[maxPosition * 2 + 1];
		for (int i = 0; i < prices.length; i++)
			prices[i] = Rands.nextGaussian(rand, 0, var);
		Arrays.sort(prices);
		double median = prices[offset];
		for (int i = 0; i < prices.length; i++)
			prices[i] = prices[i] - median;
		
		Builder<Price> builder = ImmutableList.builder();
		for (double price : prices)
			builder.add(new Price(price));
		
		this.prices = builder.build();
	}

	/**
	 * @return offset (max absolute value position)
	 */
	public int getMaxAbsPosition() {
		return offset;
	}
	
	/**
	 * @param position
	 *            Agent's current position
	 * @param quantity
	 *            Number of goods Agent will acquire from current position
	 * @return The change resulting from modifying ones position by that amount.
	 */
	public Price getValueFromQuantity(int position, int quantity) {
		return new Price(getValueAtPosition(position + quantity).intValue()
				- getValueAtPosition(position).intValue());
	}

	/**
	 * Get a pseudo private value for the agent from holding a specific position.
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
		return prices.get(bound(position + offset, 0, prices.size() - 1));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(prices, offset);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PrivateValue))
			return false;
		PrivateValue other = (PrivateValue) obj;
		return other.offset == this.offset && other.prices.equals(prices);
	}

	@Override
	public String toString() {
		return prices.toString();
	}

}
