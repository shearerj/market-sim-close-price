package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.SubmitOrder;
import systemmanager.Consts;
import systemmanager.Scheduler;
import data.MockFundamental;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * @author yngchen, ewah
 *
 */
public class WindowAgentTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	
	@BeforeClass
	public static void setUpClass(){
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "WindowAgentTest.log"));
	}

	@Before
	public void setup(){
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new CDAMarket(sip, new Random(), TimeStamp.IMMEDIATE, 1);
		
		rand = new Random(1);
	}
	
	@Test
	public void basicWindowTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		TimeStamp time10 = new TimeStamp(10);
		Scheduler em = new Scheduler(new Random());
		
		WindowAgent agent = new MockWindowAgent(fundamental, sip, market, 10);
		
		assertEquals("Incorrect initial transactions in window", 0, 
				agent.getWindowTransactions(time).size());
		
		// populate market with a transaction
		MockBackgroundAgent background1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent background2 = new MockBackgroundAgent(fundamental, sip, market);
		em.scheduleActivity(new SubmitOrder(background1, market, BUY, new Price(111), 1, time));
		em.scheduleActivity(new SubmitOrder(background2, market, SELL, new Price(110), 1, time));
		em.executeUntil(time.plus(new TimeStamp(1)));
		
		// basic window check
		Transaction trans = market.getTransactions().get(0);
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time1).size());
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time1).get(0));
		trans = sip.getTransactions().get(0);
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time1).get(0));
	
		// checking just inside window
		trans = market.getTransactions().get(0);
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time10.minus(time1)).size());
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time10.minus(time1)).get(0));
		// check outside window
		assertEquals("Incorrect # transactions", 0, agent.getWindowTransactions(time10).size());
	}
	
	@Test
	public void delayedTransactionProcessorTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);
		TimeStamp time5 = new TimeStamp(5);
		TimeStamp time10 = new TimeStamp(10);
		Scheduler em = new Scheduler(new Random());
		Market market = new CDAMarket(sip, new Random(), new TimeStamp(5), 1);
		
		WindowAgent agent = new MockWindowAgent(fundamental, sip, market, 10);
		
		// populate market with a transaction
		MockBackgroundAgent background1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent background2 = new MockBackgroundAgent(fundamental, sip, market);
		em.scheduleActivity(new SubmitOrder(background1, market, BUY, new Price(111), 1, time));
		em.executeUntil(time1);
		em.scheduleActivity(new SubmitOrder(background2, market, SELL, new Price(110), 1, time));
		em.executeUntil(time1);
		
		em.scheduleActivity(new SubmitOrder(background1, market, BUY, new Price(104), 1, time1));
		em.executeUntil(time1.plus(time1));
		em.scheduleActivity(new SubmitOrder(agent, market, SELL, new Price(102), 1, time1));
		em.executeUntil(time1.plus(time1));
		
		// test getting transactions at time 5 - should be missing the second transaction
		em.executeUntil(time5.plus(time1));
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time5).size());
		List<Transaction> trans = agent.getWindowTransactions(time5);
		assertTrue("Transaction incorrect", trans.contains(market.getTransactions().get(0)));
		
		// check time 10
		em.executeUntil(time10.plus(time1));
		trans = agent.getWindowTransactions(time10);
		assertEquals("Incorrect # transactions", 1, trans.size());
		assertEquals("Transaction price incorrect", new Price(104), trans.get(0).getPrice());
		assertEquals("Transaction seller incorrect", agent, trans.get(0).getSeller());
		assertEquals("Transaction buyer incorrect", background1, trans.get(0).getBuyer());
		
		// check outside window (at time 11)
		em.executeUntil(new TimeStamp(12));
		trans = agent.getWindowTransactions(time10.plus(time1));
		assertEquals("Incorrect # transactions", 0, trans.size());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			randMultipleTransactionWindow();
		}
	}
	
	@Test 
	public void addTransactionTest() {
		addTransaction(market, 10, 1, 10);
		List<Transaction> transactions = market.getTransactionProcessor().getTransactions();
		assertEquals("Number of transactions is incorrect", 1, transactions.size());
	}
	
	@Test
	public void singleTransactionWindow() {
		//Instantiate a WindowAgent
		WindowAgent myAgent = new MockWindowAgent(fundamental, sip, market, 10);
		assertEquals("WindowAgent Window Length is incorrect", new TimeStamp(10), myAgent.windowLength);
		//Add a transaction to the market
		addTransaction(market, 20, 1, 10);
		//Retrieve transactions via the WindowAgent's getWindowTransactions
		List<Transaction> windowTransactions = myAgent.getWindowTransactions(new TimeStamp(15));
		//Test size of returned transactions
		assertEquals("Number of transactions is not correct", 1, windowTransactions.size());
		//Test qualities of returned transaction
		assertEquals("Quantity of transaction is not correct", 1, windowTransactions.get(0).getQuantity());
		assertEquals("Price of transaction is not correct", new Price(20), windowTransactions.get(0).getPrice());
		
		
	}
	
	@Test
	public void multipleTransactionWindow() {
		WindowAgent myAgent = new MockWindowAgent(fundamental, sip, market, 10);
		assertEquals("WindowAgent Window Length is incorrect", new TimeStamp(10), myAgent.getWindowLength());
		addTransaction(market, 40, 1, 2);  //Not Included
		addTransaction(market, 30, 1, 3);  //Not Included
		addTransaction(market, 60, 1, 5);  //Not Included
		addTransaction(market, 10, 1, 10); //Included
		addTransaction(market, 20, 1, 11); //Included
		addTransaction(market, 50, 1, 14); //Included

		List<Transaction> windowTransactions = myAgent.getWindowTransactions(new TimeStamp(15)); //Window is from (5,15]
		//Test size of returned transactions
		assertEquals("Number of transactions is not correct", 3, windowTransactions.size());
		//Test qualities of returned transactions
		assertEquals("Quantity of transaction one is not correct", 1, windowTransactions.get(0).getQuantity());
		assertEquals("Price of transaction one is not correct", new Price(10), windowTransactions.get(0).getPrice());
		assertEquals("Quantity of transaction two is not correct", 1, windowTransactions.get(1).getQuantity());
		assertEquals("Price of transaction two is not correct", new Price(20), windowTransactions.get(1).getPrice());
		
	}
	
	@Test
	public void multipleTransactionWindowLatency(){
		Market market = new CDAMarket(sip, new Random(), new TimeStamp(100), 1);
		WindowAgent myAgent = new MockWindowAgent(fundamental, sip, market, 160);
		List<Transaction> windowTransactions;
		assertEquals("WindowAgent Window Length is incorrect", new TimeStamp(160), myAgent.getWindowLength());
		
		//Create mock background agents to transact
		MockBackgroundAgent agent_S = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent_B = new MockBackgroundAgent(fundamental, sip, market);
		
		//Timestamps for the first transaction and the time to execute up to
		TimeStamp t_50 = new TimeStamp(50);
		TimeStamp t_51 = new TimeStamp(51);
		//Timestamps for the second transaction and the time to execute up to
		TimeStamp t_100 = new TimeStamp(100);
		TimeStamp t_101 = new TimeStamp(101);
		//Timestamps for important intervals to check the window
		TimeStamp t_151 = new TimeStamp(151);
		TimeStamp t_201 = new TimeStamp(201);
		TimeStamp t_251 = new TimeStamp(251);
		TimeStamp t_301 = new TimeStamp(301);

		
		Scheduler em = new Scheduler(new Random());
		//Execute first transaction
		em.scheduleActivity(new SubmitOrder(agent_S, market, SELL, new Price(40), 1, t_50));
		em.executeUntil(t_51);
		em.scheduleActivity(new SubmitOrder(agent_B, market, BUY, new Price(40), 1, t_50));
		em.executeUntil(t_51);
		//Assert that the agent can't see the transaction due to latency
		windowTransactions = myAgent.getWindowTransactions(t_51);
		assertTrue("Window Transactions should be empty", windowTransactions.isEmpty());
		
		//Execute second transaction
		em.scheduleActivity(new SubmitOrder(agent_S, market, SELL, new Price(60), 1, t_100));
		em.executeUntil(t_101);
		em.scheduleActivity(new SubmitOrder(agent_B, market, BUY, new Price(60), 1, t_100));
		em.executeUntil(t_101);
		//Assert that the agent can't see the transaction due to latency
		windowTransactions = myAgent.getWindowTransactions(t_101);
		assertTrue("Window Transactions should be empty", windowTransactions.isEmpty());
		
		//Execute up to when the first transaction comes into the window
		em.executeUntil(t_151);
		//Assert that the window returns one transaction
		windowTransactions = myAgent.getWindowTransactions(t_151);
		assertEquals("Window Transactions should be size 1", 1, windowTransactions.size());
		
		//Execute up to when the second transaction comes into the window
		em.executeUntil(t_201);
		//Assert that the window returns two transactions
		windowTransactions = myAgent.getWindowTransactions(t_201);
		assertEquals("Window Transactions should be size 2", 2, windowTransactions.size());
		
		//Execute up to when the first transaction leaves the window
		em.executeUntil(t_251);
		//Assert that the window returns one transaction
		windowTransactions = myAgent.getWindowTransactions(t_251);
		assertEquals("Window Transactions should be size 1", 1, windowTransactions.size());
		
		//Execute up to when all transactions leave the window
		em.executeUntil(t_301);
		//Assert that the window returns no transactions
		windowTransactions = myAgent.getWindowTransactions(t_301);
		assertTrue("Window Transactions should be empty", windowTransactions.isEmpty());
	}
	
	
	@Test
	public void randMultipleTransactionWindow(){
		//Window length
		int windowLength = rand.nextInt(100);     
		//Re-entry Time must be greater than window length
		int reentryTime = windowLength + rand.nextInt(100);
		//Instantiate WindowAgent
		WindowAgent myAgent = new MockWindowAgent(fundamental, sip, market, windowLength);
		assertEquals("WindowAgent Window Length is incorrect", new TimeStamp(windowLength), myAgent.getWindowLength());
		
		//Keep track of how many transactions should be in the window
		int numWindow = 0;             
		int[] transactionTimes = new int[10];
		for(int j = 0; j < 10; j++){
			//Transaction times must be before re entry time
			transactionTimes[j] = rand.nextInt(reentryTime);       
			if(transactionTimes[j] > reentryTime - windowLength){
				//If transaction time falls in the window, increment
				numWindow++;           
			}
		}
		//Sort transaction times into ascending order so we can add transactions in the proper order
		Arrays.sort(transactionTimes); 
		String transaction_list = "";
		for(int t : transactionTimes){
			transaction_list+= t + " ";
			addTransaction(market, 100, 1, t);
		}
		Logger.log(Logger.Level.DEBUG, "Transaction times: " + transaction_list + 
				" Window Length: " + windowLength + " Re entry Time " + reentryTime );

		List<Transaction> windowTransactions = myAgent.getWindowTransactions(new TimeStamp(reentryTime));
		assertEquals("Number of transactions is not correct", numWindow, windowTransactions.size());
		assertEquals("Price of transactions is not correct", new Price(100), 
				windowTransactions.get(rand.nextInt(numWindow)).getPrice());
	}
	
	//Testing methods==============================================================================

	private void addTransaction(Market m, int p, int q, int time) {
		TimeStamp t = new TimeStamp(time);
		TimeStamp t_exec = new TimeStamp(time+1);
		MockBackgroundAgent agent_S = new MockBackgroundAgent(fundamental, sip, m);
		MockBackgroundAgent agent_B = new MockBackgroundAgent(fundamental, sip, m);

		Scheduler em = new Scheduler(new Random());
		em.scheduleActivity(new SubmitOrder(agent_S, m, SELL, new Price(p), q, t));
		em.executeUntil(t_exec);
		em.scheduleActivity(new SubmitOrder(agent_B, m, BUY, new Price(p), q, t));
		em.executeUntil(t_exec);
	}
	
}
