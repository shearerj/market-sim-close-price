package entity.sip;

import entity.market.Market;

public interface MarketInfo {

	public BestBidAsk getNBBO();
	
	public void processMarket(Market market);
	
}
