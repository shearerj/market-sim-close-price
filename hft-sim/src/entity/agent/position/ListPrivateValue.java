package entity.agent.position;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;

import java.util.Collections;
import java.util.List;

import utils.Rand;
import utils.SummStats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

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
 * Returns the private benefit of either buying or selling 1 unit (indicated by
 * type) when starting at the specified current position.
 * 
 * If current position is already at the max allowed, or if the position exceeds
 * the max position allowed, return +/- infinity, depending on whether
 * long/short (negative infinity for long positions, positive infinity for short
 * positions, since private values are sorted in decreasing order).
 * 
 * Note: A weird case is when the initial position is already outside the valid
 * range, in which case it will return 0 for all transitions up to the valid
 * range, after it sums as before. Agents, however, should always perform a
 * check to verify that they will not exceed their max position, before
 * submitting any orders.
 * 
 * @author ewah
 */
public class ListPrivateValue extends AbstractQuantityIndexedArray<Price> implements PrivateValue {

	private static final long serialVersionUID = -348702049295080442L;
	
	protected ListPrivateValue(List<Price> values) {
		super(values);
	}
	
	public static ListPrivateValue create(Iterable<? extends Price> values) {
		return new ListPrivateValue(ImmutableList.<Price> copyOf(values));
	}
	
	/**
	 * Randomly generate private values for positional changes
	 * 
	 * Values are generated from iid gaussian draws and then sorted.
	 */
	public static ListPrivateValue createRandomly(int maxPosition, double var, Rand rand) {
		checkArgument(maxPosition > 0, "Max Position must be positive");

		List<Price> randValues = Lists.newArrayListWithCapacity(maxPosition * 2);
		for (int i = 0; i < maxPosition * 2; i++)
			randValues.add(Price.of(rand.nextGaussian(0, var)));
		Collections.sort(randValues, Ordering.natural().reverse());
		
		return new ListPrivateValue(randValues); 
	}
	
	@Override
	public Price lowerBound() {
		return Price.INF;
	}

	@Override
	public Price upperBound() {
		return Price.NEG_INF;
	}
	
	/** Checks that the quantities are within the range to add; otherwise ignores */
	@Override
	public Price getValue(int currentPosition, int quantity, OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		int step = type.equals(BUY) ? 1 : -1;
		int nextPosition = currentPosition + quantity * step;
		if (nextPosition > getMaxAbsPosition())
			return upperBound();
		if (nextPosition < -getMaxAbsPosition())
			return lowerBound();

		double privateValue = 0;
		for (int i = 0; i < quantity; ++i)
			privateValue += getValue(currentPosition + i * step, type).doubleValue();
		return Price.of(privateValue);
	}

	@Override
	public Price getMean() {
		SummStats pv = SummStats.on();
		for (Price price : getList())
			pv.add(price.doubleValue());
		return Price.of(pv.mean());
	}

}