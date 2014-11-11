package entity.sip;

import entity.View;
import entity.market.Market;
import entity.market.Quote;

public interface MarketInfo extends View {

	public BestBidAsk getNBBO();
	
	public void quoteSubmit(Market market, Quote quote);
	
}
