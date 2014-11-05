package entity.agent.position;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import utils.Rand;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

import fourheap.Order.OrderType;

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
 * Gets margin for single-unit trades. If the projected position would
 * exceed the maximum, the profit margin is 0.
 * 
 * Margins: <code>getValue</code> is based on current (or projected) position
 * balance. <code>setValue</code> is similar.
 * 
 * NOTE: Margins only work with single quantity changes.
 * 
 * @author ewah
 *
 */
public class Margin extends AbstractQuantityIndexedArray<Double> {

	private static final long serialVersionUID = -3749423779545857329L;
	
	protected Margin(List<Double> values) {
		super(values);
	}
	
	public static Margin createRandomly(int maxPosition, Rand rand, double a, double b) {
		checkArgument(maxPosition > 0, "Max Position must be positive");
		
		List<Double> values = Lists.newArrayListWithCapacity(maxPosition * 2);
		for (int i = 0; i < maxPosition; i++)
			values.add(rand.nextUniform(a, b));
		// margins for buy orders are negative
		for (int i = 0; i < maxPosition; i++)
			values.add(-rand.nextUniform(a, b));
		
		return new Margin(values);
	}
	
	public static Margin create(Iterable<? extends Double> values) {
		return new Margin(Lists.newArrayList(values));
	}
	
	public static Margin create(double... values) {
		return new Margin(Doubles.asList(values));
	}
	
	@Override
	protected Double lowerBound() {
		return 0d;
	}

	@Override
	protected Double upperBound() {
		return 0d;
	}

	@Override
	public Double getValue(int currentPosition, int quantity,
			OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		
		// TODO need to implement for multiple units
		throw new IllegalStateException("Not implemented");
	}
}