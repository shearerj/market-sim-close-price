package entity.agent.position;

import entity.market.Price;
import fourheap.Order.OrderType;

public interface PrivateValue {

	/** Returns the maximum absolute position this is valid for */
	public int getMaxAbsPosition();
	
	/**
	 * If new position (current position +/- 1) exceeds max position, return 0.
	 * 
	 * @param currentPosition
	 *			Agent's current position
	 * @param type
	 * 			  Buy or Sell
	 * @return The new value if buying or selling 1 unit
	 */
	public Price getValue(int currentPosition, OrderType type);

	public Price getValue(int currentPosition, int quantity, OrderType type);
	
	/** Return mean of PVs for controlled varieties */
	public Price getMean();
	
}
