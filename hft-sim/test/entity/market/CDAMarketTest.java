package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static utils.Tests.assertQuote;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import utils.Mock;
import data.Props;
import entity.agent.Agent;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import event.Timeline;
import fourheap.Order.OrderType;

public class CDAMarketTest {
	private static final Random rand = new Random();
	private static final Agent agent = Mock.agent();
	
	private Timeline timeline;
	private Market market;
	private MarketView view;
	
	@Before
	public void setup() throws IOException {
		timeline = Mock.timeline;
		market = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
	}
	
	@Test
	public void submitClearTest() {
		submitOrder(BUY, Price.of(100), 1);
		submitOrder(SELL, Price.of(100), 1);
		
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(1, view.getTransactions().size());
	}
	
	@Test
	public void withdrawUpdateTest() {
		OrderRecord order = submitOrder( BUY, Price.of(100), 1);
		
		assertQuote(view.getQuote(), Price.of(100), 1, null, 0);
		
		withdrawOrder(order);
		
		assertQuote(view.getQuote(), null, 0, null, 0);
	}
	
	private void withdrawOrder(OrderRecord order) {
		market.withdrawOrder(order, order.getQuantity());
	}
	
	private OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(market.getPrimaryView(), timeline.getCurrentTime(), buyOrSell, price, quantity);
		market.submitOrder(market.getPrimaryView(), agent.getView(market.getPrimaryView().getLatency()), order);
		return order;
	}
	
}
