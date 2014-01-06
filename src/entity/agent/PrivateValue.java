package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static entity.market.Price.ZERO;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import utils.Rands;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import entity.market.Price;
import fourheap.Order.OrderType;

/**
 * PRIVATEVALUE
 * 
 * Encapsulation of an agent's private value. <code>getValueFromQuantity</code>
 * is the main method that should be used.
 * 
 * For multi-unit orders, consecutive private value transitions are summed. If
 * the quantity will exceed the maximum (offset), it will be summed up to the 
 * last valid element in values, after which it adds zero for each additional 
 * unit.
 * 
 * Note: A weird case is when the initial position is already outside the
 * valid range, in which case it will return 0 for all transitions up to the
 * valid range, after it sums as before. Agents, however, should always perform
 * a check to verify that they will not exceed their max position, before
 * submitting any orders.
 * 
 * @author ewah
 */
class PrivateValue implements Serializable, QuantityIndexedArray<Price> {

	private static final long serialVersionUID = -348702049295080442L;

	/**
	 * Constructor for an agent without private value. This will return
	 * Price.ZERO for every private value query.
	 */
	public PrivateValue() {
		super(0, Price.ZERO);
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
		super(maxPosition, Price.ZERO);

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
	protected PrivateValue(int maxPosition, Collection<Price> values) {
		super(maxPosition, Price.ZERO, values);
	}
	
	
	/**
	 * @param currentPosition
	 * @param quantity
	 * @param type
	 * @return
	 */
	public Price getValueFromQuantity(int currentPosition, int quantity, OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		int privateValue = 0;
		
		switch (type) {
		case BUY:
			for (int i = 0; i < quantity; i++)
				privateValue += getValue(currentPosition + i, type).intValue();
			break;
		case SELL:
			for (int i = 0; i < quantity; i++)
				privateValue += getValue(currentPosition - i, type).intValue();
			break;
		}
		return new Price(privateValue);
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
