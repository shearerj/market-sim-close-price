package entity.agent.position;

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
public interface QuantityIndexedArray<T> extends Serializable {

	/** Returns the maximum absolute position this is valid for */
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

	public T getValue(int currentPosition, int quantity, OrderType type);
	
	public void setValue(int currentPosition, OrderType type, T value);
}
