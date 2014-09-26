package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import activity.Clear;
import activity.SubmitOrder;
import systemmanager.Consts;
import systemmanager.Executor;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

public class BackgroundAgentTest {

	private Executor exec;
	private Market market;
	private SIP sip;

	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "BackgroundAgentTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}

	@Test
	public void getValuationBasic() {
		TimeStamp time = TimeStamp.ZERO;
		FundamentalValue randFundamental = FundamentalValue.create(0.2, 100000, 10000, new Random());

		BackgroundAgent agent = new MockBackgroundAgent(exec, randFundamental, sip, market);

		// Verify valuation (where PV = 0)
		Price val = agent.getValuation(BUY, time);
		assertEquals(randFundamental.getValueAt(time), val);
		val = agent.getValuation(SELL, time);
		assertEquals(randFundamental.getValueAt(time), val);
	}

	@Test
	public void getValuationConstPV() {
		TimeStamp time = TimeStamp.ZERO;
		List<Price> values = Arrays.asList(new Price(100), new Price(10));
		PrivateValue pv = new DummyPrivateValue(1, values);
		FundamentalValue fundamental = new MockFundamental(100000);

		BackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market, pv, 0, 1000);

		// Verify valuation (current position of 0)
		Price fund = fundamental.getValueAt(time);
		Price val = agent.getValuation(BUY, time);
		assertEquals(fund.intValue() + 10, val.intValue());
		val = agent.getValuation(SELL, time);
		assertEquals(fund.intValue() + 100, val.intValue());
	}


	@Test
	public void getValuationRand() {
		// Testing with randomized values
		TimeStamp time = TimeStamp.ZERO;
		FundamentalValue randFundamental = FundamentalValue.create(0.2, 100000, 10000, new Random());
		PrivateValue pv = new PrivateValue(5, 1000000, new Random());

		BackgroundAgent agent = new MockBackgroundAgent(exec, randFundamental, sip, market, pv, 0, 1000);

		// Get valuation for various positionBalances
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		int pv4 = pv.values.get(4).intValue();
		int pv5 = pv.values.get(5).intValue();
		int pv6 = pv.values.get(6).intValue();
		int pv7 = pv.values.get(7).intValue();
		int pv8 = pv.values.get(8).intValue();
		int pv9 = pv.values.get(9).intValue();

		agent.positionBalance = 3;
		Price fund = randFundamental.getValueAt(time);
		Price val = agent.getValuation(BUY, time);
		assertEquals(fund.intValue() + pv8, val.intValue());
		assertEquals(fund.intValue()*2 + pv9 + pv8, 
				agent.getValuation(BUY, 2, time).intValue());
		val = agent.getValuation(SELL, time);
		assertEquals(fund.intValue() + pv7, val.intValue());
		assertEquals(fund.intValue()*2 + pv7 + pv6, 
				agent.getValuation(SELL, 2, time).intValue());

		agent.positionBalance = -2;
		fund = randFundamental.getValueAt(time);
		val = agent.getValuation(BUY, time);
		assertEquals(fund.intValue() + pv3, val.intValue());
		assertEquals(fund.intValue()*3 + pv3 + pv4 + pv5, 
				agent.getValuation(BUY, 3, time).intValue());
		val = agent.getValuation(SELL, time);
		assertEquals(fund.intValue() + pv2, val.intValue());
		assertEquals(fund.intValue()*3 + pv2 + pv1 + pv0, 
				agent.getValuation(SELL, 3, time).intValue());
	}

	@Test
	public void getLimitPriceRand() {
		// Testing with randomized values
		TimeStamp time = TimeStamp.ZERO;
		FundamentalValue randFundamental = FundamentalValue.create(0.2, 100000, 10000, new Random());
		PrivateValue pv = new PrivateValue(5, 1000000, new Random());

		BackgroundAgent agent = new MockBackgroundAgent(exec, randFundamental, sip, market, pv, 0, 1000);

		// Get valuation for various positionBalances
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		int pv4 = pv.values.get(4).intValue();
		int pv5 = pv.values.get(5).intValue();
		int pv6 = pv.values.get(6).intValue();
		int pv7 = pv.values.get(7).intValue();
		int pv8 = pv.values.get(8).intValue();
		int pv9 = pv.values.get(9).intValue();
		
		agent.positionBalance = 3;
		Price fund = randFundamental.getValueAt(time);
		assertEquals(new Price(((double) fund.intValue()*2 + pv9 + pv8) / 2),
				agent.getLimitPrice(BUY, 2, time));
		assertEquals(new Price(((double) fund.intValue()*2 + pv7 + pv6) / 2),
				agent.getLimitPrice(SELL, 2, time));

		agent.positionBalance = -2;
		fund = randFundamental.getValueAt(time);
		assertEquals(new Price(((double) fund.intValue()*3 + pv3 + pv4 + pv5) / 3),
				agent.getLimitPrice(BUY, 3, time));
		assertEquals(new Price(((double) fund.intValue()*3 + pv2 + pv1 + pv0) / 3),
				agent.getLimitPrice(SELL, 3, time));
	}

	@Test
	public void processTransaction() {
		TimeStamp time = TimeStamp.ZERO;
		FundamentalValue fundamental = new MockFundamental(100000);
		FundamentalValue randFundamental = FundamentalValue.create(0.2, 100000, 10000, new Random());
		PrivateValue pv = new PrivateValue(5, 1000000, new Random());

		MockAgent agent2 = new MockAgent(exec, fundamental, sip, market);
		BackgroundAgent agent = new MockBackgroundAgent(exec, randFundamental, sip, market, pv, 0, 1000);
		assertEquals( 0, agent.transactions.size());
		assertEquals(0, agent.positionBalance);
		assertEquals(0, agent2.positionBalance);

		// Creating and adding bids
		market.submitOrder(agent, BUY, new Price(110000), 1, time);
		market.submitOrder(agent2, SELL, new Price(100000), 1, time);
		assertEquals(0, market.getTransactions().size());

		// Testing the market for the correct transactions
		market.clear(time);
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals( 1, agent.transactions.size());
		assertEquals(tr, agent.transactions.get(0));
		assertEquals( 1, agent.positionBalance );
		assertEquals( -110000, agent.profit );
		assertEquals( -1, agent2.positionBalance );
		assertEquals( 110000, agent2.profit );

		// Check surplus
		int val = agent.getTransactionValuation(BUY, time).intValue();
		assertEquals(val - 110000, agent.surplus.getValueAtDiscount(Consts.DiscountFactor.NO_DISC), 0.001);
	}

	@Test
	public void processTransactionMultiQuantity() {
		TimeStamp time = TimeStamp.ZERO;
		FundamentalValue fundamental = new MockFundamental(100000);
		FundamentalValue randFundamental = FundamentalValue.create(0.2, 100000, 10000, new Random());
		PrivateValue pv = new PrivateValue(5, 1000000, new Random());

		MockAgent agent2 = new MockAgent(exec, fundamental, sip, market);
		BackgroundAgent agent = new MockBackgroundAgent(exec, randFundamental, sip, market, pv, 0, 1000);
		assertEquals( 0, agent.transactions.size());
		assertEquals(0, agent.positionBalance);
		assertEquals(0, agent2.positionBalance);

		// Creating and adding bids
		market.submitOrder(agent, BUY, new Price(110000), 3, time);
		market.submitOrder(agent2, SELL, new Price(100000), 2, time);
		assertEquals(0, market.getTransactions().size());

		// Testing the market for the correct transactions
		market.clear(time);
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals( 1, agent.transactions.size());
		assertEquals(tr, agent.transactions.get(0));
		assertEquals( 2, agent.positionBalance );
		assertEquals( -220000, agent.profit );
		assertEquals( -2, agent2.positionBalance );
		assertEquals( 220000, agent2.profit );

		// Check surplus
		int val = agent.getTransactionValuation(BUY, 2, time).intValue();
		assertEquals(val - 220000, agent.surplus.getValueAtDiscount(Consts.DiscountFactor.NO_DISC), 0.001);
	}

	@Test
	public void getTransactionValuationRand() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);
		FundamentalValue fundamental = new MockFundamental(100000);
		FundamentalValue randFundamental = FundamentalValue.create(0.2, 100000, 10000, new Random());
		PrivateValue pv = new PrivateValue(5, 1000000, new Random());

		MockAgent agent2 = new MockAgent(exec, fundamental, sip, market);
		BackgroundAgent agent = new MockBackgroundAgent(exec, randFundamental, sip, market, pv, 0, 1000);
		assertEquals( 0, agent.transactions.size());

		// Get valuation for various positionBalances
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		int pv4 = pv.values.get(4).intValue();
		int pv5 = pv.values.get(5).intValue(); // +1
		int pv6 = pv.values.get(6).intValue(); // +2
		int pv7 = pv.values.get(7).intValue(); // +3
		int pv8 = pv.values.get(8).intValue(); // +4
		int pv9 = pv.values.get(9).intValue(); // +5

		// Creating and adding bids
		market.submitOrder(agent, BUY, new Price(110000), 1, time);
		market.submitOrder(agent2, SELL, new Price(100000), 1, time1);
		assertEquals(0, market.getTransactions().size());
		
		// Post-trans balance is 4 or 5 but before the buy transacted it was 3
		Price fund = randFundamental.getValueAt(time1);
		agent.positionBalance = 4;
		Price val = agent.getTransactionValuation(BUY, time1);
		Price val2 = agent.getTransactionValuation(BUY, time1);
		assertEquals(fund.intValue() + pv8, val.intValue());
		assertEquals(fund.intValue() + pv8, val2.intValue());
		assertEquals(4, agent.positionBalance);
		agent.positionBalance = 5;
		assertEquals(fund.intValue()*2 + pv9 + pv8, 
				agent.getTransactionValuation(BUY, 2, time1).intValue());
		assertEquals(5, agent.positionBalance);
		// Post-trans balance is 2 or 1 but before the sell transacted it was 3
		agent.positionBalance = 2;
		val = agent.getTransactionValuation(SELL, time1);
		assertEquals(fund.intValue() + pv7, val.intValue());
		agent.positionBalance = 1;
		assertEquals(fund.intValue()*2 + pv7 + pv6, 
				agent.getTransactionValuation(SELL, 2, time1).intValue());

		// Post-trans balance is -1 or 1 but before the buy transacted it was -2
		agent.positionBalance = -1;
		fund = randFundamental.getValueAt(time1);
		val = agent.getTransactionValuation(BUY, time1);
		assertEquals(fund.intValue() + pv3, val.intValue());
		agent.positionBalance = 1;
		assertEquals(fund.intValue()*3 + pv3 + pv4 + pv5, 
				agent.getTransactionValuation(BUY, 3, time1).intValue());
		// Post-trans balance is -3 or -5 but before the sell transacted it was -2
		agent.positionBalance = -3;
		val = agent.getTransactionValuation(SELL, time1);
		assertEquals(fund.intValue() + pv2, val.intValue());
		agent.positionBalance = -5;
		assertEquals(fund.intValue()*3 + pv2 + pv1 + pv0, 
				agent.getTransactionValuation(SELL, 3, time1).intValue());
	}

	@Test
	public void extraTest() {
		setup();
		getValuationRand();
		setup();
		getLimitPriceRand();
		setup();
		getTransactionValuationRand();
	}

	/**
	 * Verify do not submit order if exceed max position allowed.
	 * 
	 * XXX much of this is tested within ZIAgentTest, may want to move it here
	 */
	@Test
	public void testZIStrat() {
		log.log(DEBUG, "Testing execution of ZI strategy");
		TimeStamp time = TimeStamp.ZERO;
		List<Price> values = Arrays.asList(new Price(100), new Price(10));
		PrivateValue pv = new DummyPrivateValue(1, values);
		FundamentalValue fundamental = new MockFundamental(100000);

		BackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market, pv, 0, 1000);

		agent.executeZIStrategy(BUY, 5, time);
		assertTrue(agent.activeOrders.isEmpty());
		assertTrue(agent.transactions.isEmpty());
		agent.executeZIStrategy(SELL, 5, time);
		assertTrue(agent.activeOrders.isEmpty());
		assertTrue(agent.transactions.isEmpty());

		// Test ZI strategy
		agent.executeZIStrategy(BUY, 1, time);
		assertEquals(1, agent.activeOrders.size());

		// Verify that no other buy orders submitted, because would exceed max position
		agent.positionBalance = 1;
		agent.executeZIStrategy(BUY, 1, TimeStamp.create(1));
		assertEquals(1, agent.activeOrders.size());
		agent.executeZIStrategy(SELL, 1, TimeStamp.create(1));
		assertEquals(2, agent.activeOrders.size());
	}
	
	@Test
	public void testSubmitBuyOrder() {
		// Verify that when submit order, if would exceed position limits, then
		// do not submit the order
		PrivateValue pv = new MockPrivateValue(1);
		FundamentalValue fundamental = new MockFundamental(100000);
		
		MockAgent agent2 = new MockAgent(exec, fundamental, sip, market);
		BackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market, pv, 0, 1000);
		
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(50), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(40), 1));
		exec.executeActivity(new Clear(market));
		
		assertEquals(0, agent1.activeOrders.size());
		assertEquals(1, agent1.positionBalance);
		
		// Verify that a new buy order can't be submitted
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(50), 1));
		assertEquals(0, agent1.activeOrders.size());
		assertEquals(1, agent1.positionBalance);
		
		// Verify that a new sell order CAN be submitted
		exec.executeActivity(new SubmitOrder(agent1, market, SELL, new Price(45), 1));
		assertEquals(1, agent1.activeOrders.size());
		assertEquals(1, agent1.positionBalance);
	}
	
	@Test
	public void testSubmitSellOrder() {
		// Verify that when submit order, if would exceed position limits, then
		// do not submit the order
		PrivateValue pv = new MockPrivateValue(1);
		FundamentalValue fundamental = new MockFundamental(100000);
		
		MockAgent agent2 = new MockAgent(exec, fundamental, sip, market);
		BackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market, pv, 0, 1000);
		
		exec.executeActivity(new SubmitOrder(agent2, market, BUY, new Price(50), 1));
		exec.executeActivity(new SubmitOrder(agent1, market, SELL, new Price(40), 1));
		exec.executeActivity(new Clear(market));
		
		assertEquals(0, agent1.activeOrders.size());
		assertEquals(-1, agent1.positionBalance);
		
		// Verify that a new sell order can't be submitted
		exec.executeActivity(new SubmitOrder(agent1, market, SELL, new Price(50), 1));
		assertEquals(0, agent1.activeOrders.size());
		assertEquals(-1, agent1.positionBalance);
		
		// Verify that a new buy order CAN be submitted
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(45), 1));
		assertEquals(1, agent1.activeOrders.size());
		assertEquals(-1, agent1.positionBalance);
	}
	
	@Test
	public void testPayoff() {
		// Verify that post-liquidation, payoff includes liquidation
		PrivateValue pv = new DummyPrivateValue(1, ImmutableList.of(
				new Price(1000), new Price(-1000)));
		FundamentalValue fundamental = new MockFundamental(100000);
		
		MockAgent agent2 = new MockAgent(exec, fundamental, sip, market);
		BackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market, pv, 0, 1000);
		
		exec.executeActivity(new SubmitOrder(agent2, market, BUY, new Price(51000), 1));
		exec.executeActivity(new SubmitOrder(agent1, market, SELL, new Price(41000), 1));
		exec.executeActivity(new Clear(market));
		
		assertEquals(0, agent1.activeOrders.size());
		assertEquals(-1, agent1.positionBalance);
		// background agent sells at 51, valuation is 101, surplus is (51 - 101)
		assertEquals(-50000, agent1.getPayoff(), 0.001);
		// mock agent payoff is just profit
		assertEquals(-51000, agent2.getPayoff(), 0.001);
		
		
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(95000), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(41000), 1));
		exec.executeActivity(new Clear(market));
		assertEquals(0, agent1.activeOrders.size());
		assertEquals(0, agent1.positionBalance);
		// background agent buys at 95, valuation is 101, surplus is (101 - 95)
		assertEquals(-50000 + 6000, agent1.getPayoff(), 0.001);
		// mock agent payoff is just profit
		assertEquals(-51000 + 95000, agent2.getPayoff(), 0.001);
	}
}
