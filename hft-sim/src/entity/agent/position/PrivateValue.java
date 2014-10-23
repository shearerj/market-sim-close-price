package entity.agent.position;

import entity.market.Price;

public interface PrivateValue extends QuantityIndexedArray<Price> {

	/** Return mean of PVs for controlled varieties */
	public Price getMean();
	
}
