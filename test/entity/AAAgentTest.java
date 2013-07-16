package entity;

import static entity.AAAgent.AGGRESSION_KEY;
import static entity.AAAgent.BUYERSTATUS_KEY;
import static entity.AAAgent.DEBUG_KEY;
import static entity.AAAgent.ETA_KEY;
import static entity.AAAgent.HISTORICAL_KEY;
import static entity.AAAgent.THETAMAX_KEY;
import static entity.AAAgent.THETAMIN_KEY;
import static entity.AAAgent.THETA_KEY;
import static logger.Logger.log;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import logger.Logger;
import market.PQBid;
import market.Price;
import model.MockMarketModel;
import model.MarketModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.ProcessQuote;
import activity.SendToIP;
import activity.SubmitNMSBid;
import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import data.Keys;
import event.TimeStamp;

public class AAAgentTest {

	private static FundamentalValue fund;
	private static RandPlus rand;
	private static EntityProperties agentProperties;

	private MockMarketModel model;
	private Market market;
	private int agentIndex;
	public SIP dummySIP;

	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, "simulations/unit_testing", "AA_unit_tests.txt", true);

		// Creating the setup properties
		rand = new RandPlus(1);
		fund = new DummyFundamental(100000);

		// Setting up agentProperties
		agentProperties = new EntityProperties();
		agentProperties.put(Keys.REENTRY_RATE, 0.25);
		agentProperties.put(Keys.MAX_QUANTITY, 10);
		agentProperties.put(DEBUG_KEY, false);
		agentProperties.put(ETA_KEY, 3);
		agentProperties.put(HISTORICAL_KEY, 5);
		agentProperties.put(AGGRESSION_KEY, 0);
		agentProperties.put(THETA_KEY, 0);
		agentProperties.put(THETAMAX_KEY, 4);
		agentProperties.put(THETAMIN_KEY, -4);
	}

	@Before
	public void setupTest() {
		// Creating the MockMarketModel
		model = new MockMarketModel(1);

		// Creating the MockMarket
		market = new MockMarket(model);

		model.addMarket(market);
	}

	private AAAgent addAgent(MarketModel model, Market market, boolean isBuyer) {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(BUYERSTATUS_KEY, isBuyer);

		AAAgent agent = new AAAgent(agentIndex++, new TimeStamp(0), model,
				market, rand, testProps);

		return agent;
	}

	private void addBid(MarketModel model, Market market, Price price, int q,
			TimeStamp ts) {
		// creating a dummy agent
		MockAgent agent = new MockAgent(agentIndex++, model, market);
		// Having the agent submit a bid to the market
		market.submitBid(agent, price, q, ts);
	}

	private void addTransaction(MarketModel model, Market market, Price p,
			int q, TimeStamp time) {
		addBid(model, market, p, q, time);
		addBid(model, market, p, -q, time);
		Collection<? extends Activity> clearActs = market.clear(time);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
		Collection<Activity> sendActs = new ArrayList<Activity>();
		for (Activity act : clearActs)
			if (act instanceof SendToIP) sendActs.addAll(act.execute(time));
		for (Activity act : sendActs)
			if (act instanceof ProcessQuote) act.execute(time);
	}

	@Test
	public void initialBuyer() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing buyer on empty market: Result should be price=0");
		// Creating a buyer
		AAAgent agent = addAgent(model, market, true);
		// Testing against an empty market
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		assertTrue(bid.bidTreeSet.first().getPrice().equals(new Price(0)));
		assertTrue(bid.bidTreeSet.first().getQuantity() > 0);
	}

	@Test
	public void initialSeller() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing seller on empty market: Result should be price="
						+ Price.INF);
		// Creating a seller
		AAAgent agent = addAgent(model, market, false);
		// Testing against an empty market
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid != null);
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		assertTrue(bid.bidTreeSet.first().getPrice().equals(Price.INF));
		assertTrue(bid.bidTreeSet.first().getQuantity() < 0);
	}

	@Test
	public void noTransactionsBuyer() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing buyer on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "50000 < Bid price < 100000");

		// Setting up the bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(200000), -1, new TimeStamp(10));

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = addAgent(model, market, true);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(50000);
		Price high = new Price(100000);
		assertTrue(bid.bidTreeSet.first().getPrice().greaterThan(low));
		assertTrue(bid.bidTreeSet.first().getPrice().lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() > 0);
	}

	@Test
	public void noTransactionsSeller() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting seller on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "100000 < Ask price < 200000");

		// Adding setup bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(200000), -1, new TimeStamp(10));

		// Creating the agent and running the test
		AAAgent agent = addAgent(model, market, false);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(165000);
		Price high = new Price(170000);
		assertTrue(bid.bidTreeSet.first().getPrice().greaterThan(low));
		assertTrue(bid.bidTreeSet.first().getPrice().lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() < 0);
	}

	@Test
	public void IntraBuyerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(75000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(model, market, true);
		agent.setAggression(-1);
		Logger.log(Logger.Level.DEBUG, "Price ~= 33334");
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(30000);
		Price high = new Price(35000);
		assertTrue(bid.bidTreeSet.first().getPrice().greaterThan(low));
		assertTrue(bid.bidTreeSet.first().getPrice().lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() > 0);
	}

	@Test
	public void IntraBuyerNegative() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(75000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(model, market, true);
		agent.setAggression(-0.5);
		agent.setAdaptivness(-3.0);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(36000);
		Price high = new Price(39000);
		Price bidPrice = bid.bidTreeSet.first().getPrice();
		assertTrue(bidPrice.toString(), bidPrice.greaterThan(low));
		assertTrue(bidPrice.toString(), bidPrice.lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() > 0);
	}

	@Test
	public void IntraBuyerActive() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing active buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(75000), 1, new TimeStamp(20));

		AAAgent agent = addAgent(model, market, true);
		Logger.log(Logger.Level.DEBUG, "Price ~= 58333");
		agent.setAggression(0);
		TimeStamp ts = new TimeStamp(110);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(50000);
		Price high = new Price(60000);
		assertTrue(bid.bidTreeSet.first().getPrice().toString(),
				bid.bidTreeSet.first().getPrice().greaterThan(low));
		assertTrue(bid.bidTreeSet.first().getPrice().lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() > 0);
	}

	@Test
	public void IntraBuyerPositive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(75000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(model, market, true);
		agent.setAggression(0.5);
		agent.setAdaptivness(-3.0);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(64000);
		Price high = new Price(66000);
		Price bidPrice = bid.bidTreeSet.first().getPrice();
		assertTrue(bidPrice.toString(), bidPrice.greaterThan(low));
		assertTrue(bidPrice.toString(), bidPrice.lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() > 0);
	}

	@Test
	public void IntraBuyerAggressive() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing aggressive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(75000), 1, new TimeStamp(20));

		AAAgent agent = addAgent(model, market, true);
		Logger.log(Logger.Level.DEBUG, "Price ~= 66667");
		agent.setAggression(1.0);
		TimeStamp ts = new TimeStamp(120);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(65000);
		Price high = new Price(70000);
		Price bidPrice = bid.bidTreeSet.first().getPrice();
		assertTrue(bidPrice.toString(),
				bid.bidTreeSet.first().getPrice().greaterThan(low));
		assertTrue(bidPrice.toString(),
				bid.bidTreeSet.first().getPrice().lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() > 0);
	}

	@Test
	public void IntraSellerPassive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing passive seller on market with transactions");

		// Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(125000), 1, new TimeStamp(20));

		// Testing the Agent
		AAAgent agent = addAgent(model, market, false);
		Logger.log(Logger.Level.DEBUG,
				"Price ~= " + (150000 + (Price.INF.getPrice() - 150000) / 3));
		agent.setAggression(-1.0);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(
				150000 + (Price.INF.getPrice() - 150000) / 3 - 1000);
		Price high = new Price(
				150000 + (Price.INF.getPrice() - 150000) / 3 + 1000);
		Price bidPrice = bid.bidTreeSet.first().getPrice();
		assertTrue(bidPrice.toString(), bidPrice.greaterThan(low));
		assertTrue(bidPrice.toString(), bidPrice.lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() < 0);
	}

	@Test
	public void IntraSellerActive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing active seller on market with transactions");

		// Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(125000), 1, new TimeStamp(20));

		AAAgent agent = addAgent(model, market, false);
		log(Logger.Level.DEBUG, "Price ~= 141667");
		agent.setAggression(0);
		TimeStamp ts = new TimeStamp(110);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(138000);
		Price high = new Price(144000);
		Price bidPrice = bid.bidTreeSet.first().getPrice();
		assertTrue(bidPrice.toString(), bidPrice.greaterThan(low));
		assertTrue(bidPrice.toString(), bidPrice.lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() < 0);
	}

	@Test
	public void ExtraTest() {
		for (int i = 0; i < 100; i++) {
			setupTest();
			IntraSellerActive();
		}
	}

	@Test
	public void IntraSellerAggressive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing aggressive seller on market with transactions");

		// Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(125000), 1, new TimeStamp(20));

		AAAgent agent = addAgent(model, market, false);
		log(Logger.Level.DEBUG, "Price ~= 133334");
		agent.setAggression(1);
		TimeStamp ts = new TimeStamp(120);
		Collection<Activity> test = agent.agentStrategy(ts);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (SubmitNMSBid.class.isAssignableFrom(act.getClass())) {
				act.execute(ts);
			}
		}

		// Getting the bid from the market
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent().equals(agent));
		Price low = new Price(130000);
		Price high = new Price(135000);
		assertTrue(bid.bidTreeSet.first().getPrice().greaterThan(low));
		assertTrue(bid.bidTreeSet.first().getPrice().lessThan(high));
		assertTrue(bid.bidTreeSet.first().getQuantity() < 0);
	}

	@Test
	public void AggressionLearning() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing aggression learning");

		// Adding Bids and Transactions
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(100000), 1, new TimeStamp(20));
		addTransaction(model, market, new Price(100000), 1, new TimeStamp(21));
		addTransaction(model, market, new Price(100000), 1, new TimeStamp(22));
		addTransaction(model, market, new Price(100000), 1, new TimeStamp(23));
		addTransaction(model, market, new Price(100000), 1, new TimeStamp(24));

		AAAgent agent = addAgent(model, market, false);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		System.out.println(agent.getAggression());
		// Finding the bid in the activity list
		for (Activity itr : test) {
			if (SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				// Casting the bid
				// SubmitNMSBid bid = (SubmitNMSBid) itr;
				// System.out.println(bid.getPrice());
			}
		}
		assertTrue(agent.getAggression() < 0);
	}
}
