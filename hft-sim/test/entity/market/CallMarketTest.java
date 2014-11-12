package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.assertQuote;
import static utils.Tests.assertSingleTransaction;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.ClearInterval;
import systemmanager.Keys.PricingPolicy;
import utils.Mock;
import utils.Rand;
import data.Props;
import entity.agent.Agent;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import event.EventQueue;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Note that CallMarket initial Clear activities are inserted by the
 * SystemManager.executeEvents method.
 * 
 * @author ewah
 */
public class CallMarketTest {
	private static final Rand rand = Rand.create();
	private static final Agent agent = Mock.agent();
	
	private EventQueue timeline;
	private Market market;
	private MarketView view;
	
	@Before
	public void setup() {
		timeline = EventQueue.create(Log.nullLogger(), rand);
		market = CallMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs(
				ClearInterval.class,	TimeStamp.of(100),
				PricingPolicy.class,	1d));
		view = market.getPrimaryView();
		timeline.executeUntil(TimeStamp.ZERO); // First clear
	}

	/** Test modified pricing policy */
	@Test
	public void pricingPolicyTest() {
		submitOrder(BUY, Price.of(200), 1);
		submitOrder(SELL, Price.of(100), 1);
		
		// Testing market for the correct transaction
		market.clear();
		timeline.executeUntil(TimeStamp.ZERO);
		
		assertSingleTransaction(view.getTransactions(), Price.of(200), TimeStamp.ZERO, 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
	}
	
	/** Test that market clears at intervals */
	@Test
	public void clearActivityInsertion() {
		// Test that before time 100 quotes do not change
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		// Quote still undefined before clear
		submitOrder(BUY, Price.of(100),  1);
		OrderRecord sell = submitOrder(SELL, Price.of(110), 1);
		timeline.executeUntil(TimeStamp.of(99));
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		// Now quote should be updated
		timeline.executeUntil(TimeStamp.of(100));
		assertQuote(view.getQuote(), Price.of(100), 1, Price.of(110), 1);
		
		// Now check that transactions are correct as well as quotes
		submitOrder(SELL, Price.of(150), 1);
		OrderRecord buy = submitOrder(BUY, Price.of(120), 1);
		// Before second clear interval ends, quote remains the same
		timeline.executeUntil(TimeStamp.of(199));
		assertQuote(view.getQuote(), Price.of(100), 1, Price.of(110), 1);
		
		// Once clear interval ends, orders match and clear, and the quote updates
		timeline.executeUntil(TimeStamp.of(200));
		
		assertQuote(view.getQuote(), Price.of(100), 1, Price.of(150), 1);
		assertSingleTransaction(view.getTransactions(), Price.of(120), TimeStamp.of(200), 1);
		assertEquals(0, buy.getQuantity());
		assertEquals(0, sell.getQuantity());
	}
	
	private OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(market.getPrimaryView(), timeline.getCurrentTime(), buyOrSell, price, quantity);
		market.submitOrder(market.getPrimaryView(), agent.getView(market.getPrimaryView().getLatency()), order);
		return order;
	}
	
}
