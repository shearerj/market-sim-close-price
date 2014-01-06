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
public interface QuantityIndexedArray<T extends Number> extends Serializable {

	
	/**
	 * @return offset
	 */
	public int getMaxAbsPosition();
	
	/**
	 * If new position (current position +/- 1) exceeds max position, return 0.
	 * 
	 * @param currentPosition
	 *            Agent's current position
	 * @param type
	 * 			  Buy or Sell
	 * @return The new value if buying or selling 1 unit
	 */
	public T getValue(int currentPosition, OrderType type);

	public T getValueFromQuantity(int currentPosition, int quantity, OrderType type);
}
