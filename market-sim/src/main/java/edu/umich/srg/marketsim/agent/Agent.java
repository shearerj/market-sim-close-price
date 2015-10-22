package edu.umich.srg.marketsim.agent;

import com.google.gson.JsonObject;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;

public interface Agent {

	void initilaize();
	
	double payoffForPosition(int position);
	
	JsonObject getFeatures();
	
	// Notifications
	
	void notifyOrderSubmitted(OrderRecord order);

	void notifyOrderWithrawn(OrderRecord order, int quantity);
	
	void notifyOrderTransacted(OrderRecord order, Price price, int quantity);
	
	void notifyQuoteUpdated(MarketView market);
	
	void notifyTransaction(MarketView market, Price price, int quantity);
	
}
