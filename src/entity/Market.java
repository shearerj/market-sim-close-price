package entity;

/**
 * Base class for all markets.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {
	
	public Market() {
		// empty constructor
	}
	
	public Market(int marketID) {
		this.entityID = marketID;
	}
}
