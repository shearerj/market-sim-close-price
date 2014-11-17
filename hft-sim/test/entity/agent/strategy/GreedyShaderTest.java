package entity.agent.strategy;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import utils.Mock;
import entity.agent.Agent;
import entity.agent.OrderRecord;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class GreedyShaderTest {
	
	// FIXME Check that order isn't replaced if it's the exact same as previous order

	private Market market;
	private MarketView view;
	private Agent agent;
	
	@Before
	public void setup() {
		market = Mock.market();
		view = market.getPrimaryView();
		agent = Mock.agent();
	}
	
	@Test
	public void buyerShading() {
		LimitPriceEstimator estimator = new LimitPriceEstimator() {
			@Override public Price getLimitPrice(OrderType buyOrSell, int quantity) { return Price.of(25); }
		};
		GreedyShader shader = GreedyShader.create(estimator, 0.75);
		
		setQuote(null, Price.of(10));
		OrderRecord shaded = shader.apply(order(BUY, Price.of(5)));
		assertEquals(Price.of(10), shaded.getPrice());
		
		shaded = shader.apply(order(BUY, Price.of(4)));
		assertEquals(Price.of(4), shaded.getPrice());
	}
	
	@Test
	public void sellerShading() {
		LimitPriceEstimator estimator = new LimitPriceEstimator() {
			@Override public Price getLimitPrice(OrderType buyOrSell, int quantity) { return Price.of(20); }
		};
		GreedyShader shader = GreedyShader.create(estimator, 0.75);
		
		setQuote(Price.of(50), null);
		OrderRecord shaded = shader.apply(order(SELL, Price.of(60)));
		assertEquals(Price.of(50), shaded.getPrice());
		
		shaded = shader.apply(order(BUY, Price.of(61)));
		assertEquals(Price.of(61), shaded.getPrice());
	}
	
	private void setQuote(Price bid, Price ask) {
		if (bid != null)
			view.submitOrder(agent, OrderRecord.create(view, TimeStamp.ZERO, BUY, bid, 1));
		if (ask != null)
			view.submitOrder(agent, OrderRecord.create(view, TimeStamp.ZERO, SELL, ask, 1));
	}
	
	private OrderRecord order(OrderType buyOrSell, Price price) {
		return OrderRecord.create(view, TimeStamp.ZERO, buyOrSell, price, 1);
	}

}
