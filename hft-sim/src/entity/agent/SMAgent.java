package entity.agent;

import java.util.Random;

import systemmanager.Simulation;
import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * SMAGENT
 * 
 * Single market (SM) agent, whose agent strategy is executed only within one
 * market. This does not mean that it can only trade with its specified market;
 * it means that it only checks price quotes from its primary market.
 * 
 * An SMAgent is capable of seeing the quote from its own market with zero
 * delay. It also tracks to which market it has most recently submitted a bid,
 * as it is only permitted to submit to one market at a time.
 * 
 * ORDER ROUTING (REGULATION NMS):
 * 
 * The agent's order will be routed to the alternate market ONLY if both the
 * NBBO quote is better than the primary market's quote and the submitted bid
 * will transact immediately given the price in the alternate market. The only
 * difference in outcome occurs when the NBBO is out-of-date and the agent's
 * order is routed to the main market when the alternate market is actually
 * better.
 * 
 * @author ewah
 */
public abstract class SMAgent extends Agent {

	private static final long serialVersionUID = 3156640550886695881L;
	
	protected final MarketView primaryMarket;

	protected SMAgent(Simulation sim, TimeStamp arrivalTime, Random rand, Market market, Props props) {
		super(sim, arrivalTime, rand, props);
		this.primaryMarket = market.getPrimaryView();
	}
	
	protected OrderRecord submitOrder(OrderType type, Price price, int quantity) {
		return submitOrder(primaryMarket, type, price, quantity);
	}
	
	protected OrderRecord submitNMSOrder(OrderType type, Price price, int quantity) {
		return submitNMSOrder(primaryMarket, type, price, quantity);
	}

	protected Quote getQuote() {
		return primaryMarket.getQuote();
	}
	
}
