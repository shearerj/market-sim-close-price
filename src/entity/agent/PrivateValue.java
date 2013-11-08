package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static entity.market.Price.ZERO;
import static utils.MathUtils.bound;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import systemmanager.Consts.OrderType;
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
 * NOTE: This private value model is only for single-unit limit orders.
 * 
 * @author ewah
 */
public class PrivateValue implements Serializable {

	private static final long serialVersionUID = -348702049295080442L;
	
	protected final int offset;
	protected final List<Price> values;

	/**
	 * Constructor for an agent without private value. This will return
	 * Price.ZERO for every private value query.
	 */
	public PrivateValue() {
		offset = 0;
		values = Collections.singletonList(ZERO);
	}

	/**
	 * Randomly generate private values for positional changes
	 * 
	 * @param maxPosition
	 *            the maximum position the agent can take.
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
		double[] values = new double[maxPosition * 2];
		for (int i = 0; i < values.length; i++)
			values[i] = Rands.nextGaussian(rand, 0, var);
		Arrays.sort(values);
		
		Builder<Price> builder = ImmutableList.builder();
		for (double value : values)
			builder.add(new Price(value));
		
		this.values = builder.build();
	}
	
	/**
	 * Protected constructor for testing purposes (DummyPrivateValue)
	 */
	protected PrivateValue(int maxPosition, Collection<Price> values){
		checkArgument(values.size() == 2*maxPosition, "Incorrect number of entries in list");
		Builder<Price> builder = ImmutableList.builder();
		builder.addAll(values);
		this.values = builder.build();
		offset = maxPosition;
	}

	/**
	 * @return offset (max absolute value position)
	 */
	public int getMaxAbsPosition() {
		return offset;
	}
	
	/**
	 * @param currentPosition
	 *            Agent's current position
	 * @param type
	 * 			  Buy or Sell
	 * @return The new private value if buying or selling 1 unit
	 */
	public Price getValueFromQuantity(int currentPosition, OrderType type) {
		return getValueFromQuantity(currentPosition, 1, type);
	}
	
	/**
	 * @param currentPosition
	 * @param quantity
	 * @param type
	 * @return
	 */
	public Price getValueFromQuantity(int currentPosition, int quantity, OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		switch (type) {
		case BUY:
			return values.get(bound(currentPosition + offset + (quantity - 1), 0, values.size() - 1));
		case SELL:
			return values.get(bound(currentPosition + offset - 1 - (quantity - 1), 0, values.size() - 1));

		default:
			return Price.ZERO;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(values, offset);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PrivateValue))
			return false;
		PrivateValue other = (PrivateValue) obj;
		return other.offset == this.offset && other.values.equals(values);
	}

	@Override
	public String toString() {
		return values.toString();
	}

}
