package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import entity.market.Price;
import fourheap.Order.OrderType;

/**
 * For holding all order prices in an array.
 * 
 * Used by ZIP Agents, which compute the order price for every quantity.
 * 
 * @author ewah
 *
 */
public class PriceArray implements Serializable, QuantityIndexedArray<Price> {

	private static final long serialVersionUID = 5529375650074829876L;

	protected final int offset;
	protected List<Price> values;
	
	public PriceArray() {
		this.offset = 0;
		this.values = Collections.emptyList();
	}
	
	/**
	 * Initialize all values to -1.
	 * @param maxPosition
	 */
	public PriceArray(int maxPosition) {
		checkArgument(maxPosition > 0, "Max Position must be positive");
		
		// Identical to legacy generation in final output
		this.offset = maxPosition;
		this.values = Lists.newArrayList();
		
		for (int i = 0; i < maxPosition * 2; i++) {
			this.values.add(new Price(-1));
		}
	}
	
	/**
	 * Protected constructor for testing purposes.
	 * 
	 * @param maxPosition
	 * @param values
	 */
	protected PriceArray(int maxPosition, Collection<Price> values) {
		checkArgument(values.size() == 2*maxPosition, "Incorrect number of entries in list");
		this.values = Lists.newArrayList();
		this.values.addAll(values);
		offset = maxPosition;
	}
	
	@Override
	public int getMaxAbsPosition() {
		return offset;
	}

	/**
	 * If the projected position would exceed the maximum, the price is 0.
	 * 
	 * @param currentPosition
	 * @param type
	 * @return
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
	 * @param type
	 * @param value
	 */
	public void setValue(int currentPosition, OrderType type,
			Price value) {
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
	public Price getValueFromQuantity(int currentPosition, int quantity,
			OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		
		// TODO need to implement for multiple units
		return Price.ZERO;
	}
}
