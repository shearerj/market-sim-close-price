package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import fourheap.Order.OrderType;

/**
 * For storing lists of values that are indexed by quantity (and implicitly, position).
 * Examples include private values, margins, etc.
 * 
 * @author ewah
 *
 * @param <T>
 */
abstract class QuantityIndexedValue<T extends Number> implements Serializable {

	private static final long serialVersionUID = -7306551216718472620L;

	protected final int offset;
	protected List<T> values;
	protected T defaultValue;
	
	protected QuantityIndexedValue() {
		this.offset = 0;
		this.values = Collections.emptyList();
		this.defaultValue = null;
	}
	
	protected QuantityIndexedValue(int offset) {
		this(offset, null);
	}
	
	protected QuantityIndexedValue(int offset, T defaultValue) {
		this.offset = offset;
		this.defaultValue = defaultValue;
		this.values = Lists.newArrayList();
	}
	
	/**
	 * @param maxPosition
	 * @param rand
	 * @param a
	 * @param b
	 */
	protected QuantityIndexedValue(int maxPosition, T defaultValue, Collection<T> values) {
		this(maxPosition, defaultValue);
		checkArgument(offset > 0, "Max Position must be positive");
		checkArgument(values.size() == 2*maxPosition, "Incorrect number of entries in list");
		this.values.addAll(values);
	}
	
	/**
	 * @return offset
	 */
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
	 * @return The new value if buying or selling 1 unit
	 */
	public T getValue(int currentPosition, OrderType type) {
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
		return defaultValue;
	}

	/**
	 * @param currentPosition
	 * @param type
	 * @param value
	 */
	public void setValue(int currentPosition, OrderType type,
			T value) {
		switch (type) {
		case BUY:
			if (currentPosition + offset <= values.size() - 1 &&
					currentPosition + offset >= 0)
				values.add(currentPosition + offset, value);
			break;
		case SELL:
			if (currentPosition + offset - 1 <= values.size() - 1 && 
					currentPosition + offset - 1 >= 0)
				values.add(currentPosition + offset - 1, value);
			break;
		}
	}
	
//	/**
//	 * @param currentPosition
//	 * @param quantity
//	 * @param type
//	 * @return
//	 */
//	public T getValueFromQuantity(int currentPosition, int quantity, OrderType type) {
//		checkArgument(quantity > 0, "Quantity must be positive");
//		
//		// TODO need to implement for multiple units
//		return defaultValue;
//	}
}
