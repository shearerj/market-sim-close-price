package entity.agent;

import java.io.Serializable;

import fourheap.Order.OrderType;

/**
 * For storing lists of values that are indexed by quantity (and implicitly, position).
 * Examples include private values, margins, etc.
 * 
 * @author ewah
 *
 * @param <T>
 */
public interface QuantityIndexedValue<T extends Number> extends Serializable {

	/**
	 * @return offset
	 */
	public int getMaxAbsPosition();
	
	/**
	 * @param currentPosition
	 * @param type
	 * @return
	 */
	public T getValue(int currentPosition, OrderType type);

	
	/**
	 * @param currentPosition
	 * @param quantity
	 * @param type
	 * @return
	 */
	public T getValueFromQuantity(int currentPosition, int quantity, OrderType type);
}
