package entity.agent.strategy;

import entity.agent.OrderRecord;

public interface MarketMakerStrategy {

	// FIXME Might want to put upper and lower bounds or something standard to all market maker ladders that changes as a parameter
	public Iterable<OrderRecord> getLadder();
	
}
