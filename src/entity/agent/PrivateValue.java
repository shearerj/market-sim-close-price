package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static entity.market.Price.ZERO;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
public class PrivateValue implements Serializable, QuantityIndexedArray<Price> {

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
	protected PrivateValue(int maxPosition, Collection<Price> values) {
		checkArgument(values.size() == 2*maxPosition, "Incorrect number of entries in list");
		Builder<Price> builder = ImmutableList.builder();
		builder.addAll(values);
		this.values = builder.build();
		offset = maxPosition;
	}

	/**
	 * @return offset (max absolute value position)
	 */
	@Override
	public int getMaxAbsPosition() {
		return offset;
	}
	
	/**
	 * If new position (current position +/- 1) exceeds max position, return 0.
	 * 
	 * @param currentPosition
	 *            Agent's current position
	 * @param type
	 * 			  Buy or Sell
	 * @return The new private value if buying or selling 1 unit
	 */
	@Override
	public Price getValue(int currentPosition, OrderType type) {
		switch (type) {
		case BUY:
			if (currentPosition + offset <= values.size() - 1 &&
					currentPosition + offset >= 0)
				return values.get(currentPosition + offset);
			break;
		case SELL:
			if (currentPosition + offset - 1 <= values.size() - 1 && 
					currentPosition + offset - 1 >= 0)
				return values.get(currentPosition + offset - 1);
			break;
		}
		return Price.ZERO;
	}
	
	/**
	 * @param currentPosition
	 * @param quantity
	 * @param type
	 * @return
	 */
	@Override
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

	/**
	 * For control variates.
	 * @return
	 */
	public Price getMean() {
		SummaryStatistics pv = new SummaryStatistics();
		for (Price price : values) {
			pv.addValue(price.doubleValue());
		}
		return new Price(pv.getMean());
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