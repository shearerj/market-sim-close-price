package entity.agent.position;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.google.common.collect.Lists;

import fourheap.Order.OrderType;

public class Aggression extends AbstractQuantityIndexedArray<Double> {

	private Aggression(List<Double> values) {
		super(values);
	}

	public static Aggression create(int maxPosition, double initialValue) {
		checkArgument(maxPosition > 0, "Max position must be positive");

		List<Double> values = Lists.newArrayListWithCapacity(maxPosition * 2);
		for (int i = 0; i < maxPosition * 2; ++i)
			values.add(initialValue);

		return new Aggression(values);
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
		// TODO how to handle multi-quantity?
		throw new IllegalStateException("Unimplemented");
	}

	private static final long serialVersionUID = -8437580530274339226L;

}
