package entity.infoproc;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.NbboLatency;
import systemmanager.MockSim;

import com.google.common.base.Optional;

import data.Props;
import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.OrderRecord;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

public class SIPTest {
	
	// FIXME Make sure SIP is logging spread, SIP only queries spread, so it may not work in the way we want...

	/*
	 * TODO Undefined order between markets? Transactions for a market should be
	 * in proper order, but if several markets had a transaction at the same
	 * time, the order will be undefined
	 */
	private static final Random rand = new Random();
	private MockSim sim;
	private Market market1;
	private Market market2;
	private SIP sip;
	
	@Before
	public void defaultSetup() throws IOException {
		setup(Props.fromPairs());
	}
	
	public void setup(Props parameters) throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 2, parameters);

		Iterator<Market> markets = sim.getMarkets().iterator();
		market1 = markets.next();
		market2 = markets.next();
		assertFalse(markets.hasNext());
		
		sip = sim.getSIP();
	}
	
	@Test
	public void basicQuote() {
		// Test initial NBBO quote
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals(Optional.absent(), nbbo.bestAsk);
		assertEquals(Optional.absent(), nbbo.bestBid);
		assertEquals(0, nbbo.bestAskQuantity);
		assertEquals(0, nbbo.bestBidQuantity);
		assertEquals(Optional.absent(), nbbo.bestAskMarket);
		assertEquals(Optional.absent(), nbbo.bestBidMarket);
		
		setQuote(market1, Price.of(80), Price.of(100));
		
		// Test that NBBO quote is correct
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Price.of(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", Price.of(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
	}
	
	@Test
	public void multiQuote() {
		setQuote(market1, Price.of(80), Price.of(100));
		setQuote(market1, Price.of(70), Price.of(90));
		
		// Test that NBBO quote is correct (completely replaces old quote of [80, 100])
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(90)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(70)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
		assertEquals(1, sip.views.size());
	}
	
	@Test
	public void staleQuote() {
		sim.executeUntil(TimeStamp.of(10));
		setQuote(market1, Price.of(80), Price.of(100));
		
		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(100)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(80)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
		
		// Note that staleness is based solely on MarketTime (not timestamp)
		setQuote(market1, Price.of(70), Price.of(90));
		
		// Test that NBBO quote is correct (ignores stale quote q2)
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(100)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(80)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
	}
	
	@Test
	public void twoMarketQuote() {
		setQuote(market1, Price.of(80), Price.of(100));
		setQuote(market2, Price.of(70), Price.of(90));
		
		// Test that NBBO quote is correct (computes best quote between both markets)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(90)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(80)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market2), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
		assertEquals(2, sip.views.size());
	}
	
	@Test
	public void twoMarketMultiQuote() {
		setQuote(market1, Price.of(85), Price.of(100));
		setQuote(market2, Price.of(75), Price.of(95));
		setQuote(market1, Price.of(65), Price.of(90));
				
		// Test that NBBO quote is correct & that market 1's quote was replaced
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(90)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(75)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market2), nbbo.bestBidMarket);
		
		setQuote(market2, Price.of(60), Price.of(91));
		
		// Test that NBBO quote is correct & that market 2's quote was replaced
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(90)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(65)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);

		/*
		 * XXX NOTE: if tie in price, nondeterminism in which market has best
		 * price.
		 */
	}
	
	@Test
	public void basicNoDelay() {
		setQuote(market1, Price.of(80), Price.of(100));

		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(100)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(80)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 2, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
	}

	@Test
	public void basicDelay() throws IOException {
		// Check that process quote activity scheduled correctly
		setup(Props.fromPairs(NbboLatency.class, TimeStamp.of(50)));
		setQuote(market1, Price.of(80), Price.of(100));
		
		// Verify correct process quote activity added to execute at time 50
		sim.executeUntil(TimeStamp.of(49));
		// FIXME Assert not updates
		
		sim.executeUntil(TimeStamp.of(50));

		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(100)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(80)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
	}
	
	@Test
	public void basicZeroDelay() throws IOException {
		// SIP with zero not immediate latency
		setup(Props.fromPairs(NbboLatency.class, TimeStamp.ZERO));
		// Check that process quote activity scheduled correctly
		setQuote(market1, Price.of(80), Price.of(100));
		
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.absent(), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.absent(), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.absent(), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.absent(), nbbo.bestBidMarket);
		
		// Test that NBBO quote is correct after time 0
		sim.executeUntil(TimeStamp.ZERO);
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Optional.of(Price.of(100)), nbbo.bestAsk);
		assertEquals("Incorrect BID", Optional.of(Price.of(80)), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
	}
	
	@Test
	public void eventManagerLatencyTest() throws IOException {
		setup(Props.fromPairs(NbboLatency.class, TimeStamp.of(50)));
		setQuote(market1, Price.of(80), Price.of(100));
		setQuote(market2, Price.of(75), 1, Price.of(95), 2);
		
		// Check that no quotes have updated in slow sip yet (fast sip should update)
		assertEquals("Incorrect ASK", Optional.absent(), sip.getNBBO().bestAsk);
		assertEquals("Incorrect BID", Optional.absent(), sip.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.absent(), sip.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", Optional.absent(), sip.getNBBO().bestBidMarket);
		
		
		// Check immediate SIP updated with quote 1
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Price.of(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", Price.of(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
		// Check delayed SIP not updated
		assertEquals("Incorrect ASK", Optional.absent(), sip.getNBBO().bestAsk);
		assertEquals("Incorrect BID", Optional.absent(), sip.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.absent(), sip.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", Optional.absent(), sip.getNBBO().bestBidMarket);
		
		// Send more quotes to SIPs but only execute up to SIP2 latency of 100
		// so only first quote of [80, 100] should reach the delayed SIP
		sim.executeUntil(TimeStamp.of(30));
		// FIXME Still not updated?
		sim.executeUntil(TimeStamp.of(50));
		// Check immediate SIP updated with quote 2
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", Price.of(95), nbbo.bestAsk);
		assertEquals("Incorrect BID", Price.of(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 2, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market2), nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
		// Check delayed SIP updated only with quote 1
		assertEquals("Incorrect ASK", Price.of(100), sip.getNBBO().bestAsk);
		assertEquals("Incorrect BID", Price.of(80), sip.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 1, sip.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, sip.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market1), sip.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), sip.getNBBO().bestBidMarket);
		
		// Delayed SIP won't update until after 100 time steps after the second 
		// quote was submitted (which was at time 30), so need to execute up to time 130
		sim.executeUntil(TimeStamp.of(80));
		// Check delayed SIP updated finally with quote 2
		assertEquals("Incorrect ASK", Price.of(95), sip.getNBBO().bestAsk);
		assertEquals("Incorrect BID", Price.of(80), sip.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 2, sip.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, sip.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", Optional.of(market2), sip.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", Optional.of(market1), sip.getNBBO().bestBidMarket);
	}
	
	// FIXME
//	@Test
//	public void transactionsInSIP() {
//		//Creating dummy agents
//		Agent agent1 = mockAgent(market1);
//		Agent agent2 = mockAgent(market1);
//
//		// Creating and adding bids
//		market1.submitOrder(agent1, BUY, Price.of(150), 2, NO_EXPIRATION);
//		// should execute clear since CDA
//		
//		// Verify that NBBO quote has updated
//		BestBidAsk nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.absent(), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(150)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 2, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", Optional.absent(), nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
//		
//		market1.submitOrder(agent2, SELL, Price.of(140), 1, NO_EXPIRATION);
//		// should execute Clear-->SendToSIP-->processInformations
//		
//		// Verify that transactions has updated as well as NBBO
//		nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.absent(), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(150)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", Optional.absent(), nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
//		Transaction trans = Iterables.getOnlyElement(sip.getTransactions());
//		assertEquals("Incorrect transaction price", Price.of(150), trans.getPrice());
//		assertEquals("Incorrect transaction quantity", 1, trans.getQuantity());
//		assertEquals("Incorrect buyer", agent1, trans.getBuyer());
//		assertEquals("Incorrect buyer", agent2, trans.getSeller());
//	}
//	
//	@Test
//	public void transactionsInDelayedSIP() throws IOException {
//		setup(Keys.NBBO_LATENCY, 100);
//		
//		//Creating dummy agents
//		Agent agent1 = mockAgent(market1);
//		Agent agent2 = mockAgent(market1);
//
//		// Creating and adding bids
//		market1.submitOrder(agent1, BUY, Price.of(150), 2, NO_EXPIRATION);
//		// should execute clear since CDA
//		
//		// Verify that no NBBO quote yet
//		BestBidAsk nbbo = sip.getNBBO();
//		assertEquals(Optional.absent(), nbbo.bestAsk);
//		assertEquals(Optional.absent(), nbbo.bestBid);
//		assertEquals(0, nbbo.bestAskQuantity);
//		assertEquals(0, nbbo.bestBidQuantity);
//		assertEquals(Optional.absent(), nbbo.bestAskMarket);
//		assertEquals(Optional.absent(), nbbo.bestBidMarket);
//		assertEquals(0, sip.views.size());
//		
//		market1.submitOrder(agent2, SELL, Price.of(140), 1, NO_EXPIRATION);
//		// should execute Clear-->SendToSIP-->ProcessQuotes
//		
//		// Verify that transactions has updated as well as NBBO
//		sim.executeUntil(TimeStamp.of(100)); // because of SIP latency
//		nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.absent(), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(150)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", Optional.absent(), nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
//		Transaction trans = Iterables.getOnlyElement(sip.getTransactions());
//		assertEquals("Incorrect transaction price", Price.of(150), trans.getPrice());
//		assertEquals("Incorrect transaction quantity", 1, trans.getQuantity());
//		assertEquals("Incorrect buyer", agent1, trans.getBuyer());
//		assertEquals("Incorrect buyer", agent2, trans.getSeller());
//	}
//	
//	@Test
//	public void basicOrderRoutingNMS() throws IOException {
//		setup(Keys.NBBO_LATENCY, 50);
//		
//		// Set up CDA markets and their quotes
//		Agent background1 = mockAgent(market2);
//		Agent background2 = mockAgent(market1);
//		market2.submitOrder(background1, SELL, Price.of(111), 1, NO_EXPIRATION);
//		market1.submitOrder(background1, BUY, Price.of(104), 1, NO_EXPIRATION);
//		market1.submitOrder(background2, SELL, Price.of(110), 1, NO_EXPIRATION);
//		market2.submitOrder(background2, BUY, Price.of(102), 1, NO_EXPIRATION);
//
//		// Verify that NBBO quote is (104, 110) at time 50 (after quotes have been processed by SIP)
//		BestBidAsk nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.of(Price.of(110)), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(104)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
//		
//		///////////////
//		// Creating dummy agent & submit sell order
//		Agent agent1 = mockAgent(market2);
//		sim.executeUntil(TimeStamp.of(50));
//		market2.submitOrder(agent1, SELL, Price.of(105), 1, NO_EXPIRATION);
//		sim.executeUntil(TimeStamp.of(99));
//		nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.of(Price.of(110)), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(104)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", Optional.of(market1), nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
//		
//		// Verify that NBBO quote is (104, 105) at time 100 (after quotes have been processed by SIP)
//		sim.executeUntil(TimeStamp.of(100));
//		nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.of(Price.of(105)), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(104)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", Optional.of(market2), nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", Optional.of(market1), nbbo.bestBidMarket);
//		
//		// Another agent submits a buy order
//		Agent agent2 = mockAgent(market1);
//		market1.submitOrder(agent2, BUY, Price.of(109), 1, NO_EXPIRATION);
//		
//		// Verify that order is routed to nyse and transacts immediately w/ agent1's order
//		assertEquals(1, market2.getPrimaryView().getTransactions().size());
//		assertEquals(0, market1.getPrimaryView().getTransactions().size());
//		Transaction t = Iterables.getOnlyElement(market2.getPrimaryView().getTransactions());
//		assertEquals(market2, t.getMarket());
//		assertEquals(Price.of(105), t.getPrice());
//		assertEquals(agent1, t.getSeller());
//		assertEquals(agent2, t.getBuyer());
//	}
//
//	@Test
//	public void latencyArbRoutingNMS() throws IOException {
//		setup(Keys.NBBO_LATENCY, 50);
//
//		Agent background1 = mockAgent(market2);
//		Agent background2 = mockAgent(market1);
//		market2.submitOrder(background1, SELL, Price.of(111), 1, NO_EXPIRATION);
//		market1.submitOrder(background1, BUY, Price.of(104), 1, NO_EXPIRATION);
//		market1.submitOrder(background2, SELL, Price.of(110), 1, NO_EXPIRATION);
//		market2.submitOrder(background2, BUY, Price.of(102), 1, NO_EXPIRATION);
//		sim.executeUntil(TimeStamp.of(50));
//		
//		///////////////
//		// Creating dummy agent & submit sell order
//		Agent agent1 = mockAgent(market2);
//		market2.submitOrder(agent1, SELL, Price.of(105), 1, NO_EXPIRATION);
//		
//		// Verify that NBBO quote is still (104, 110) (hasn't updated yet)
//		BestBidAsk nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.of(Price.of(110)), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(104)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
//		
//		// Another agent submits a buy order
//		Agent agent2 = mockAgent(market1);
//		market1.submitNMSOrder(agent2, BUY, Price.of(109), 1, NO_EXPIRATION);
//		sim.executeUntil(TimeStamp.of(99));
//		// Verify that NBBO quote is still (104, 110) (hasn't updated yet)
//		nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.of(Price.of(110)), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(104)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
//		
//		// The buy order was still routed to Nasdaq, so the NBBO will cross and 
//		// there is a latency arbitrage opportunity
//		sim.executeUntil(TimeStamp.of(100));
//		// Verify that NBBO quote is now (109, 105)
//		nbbo = sip.getNBBO();
//		assertEquals("Incorrect ASK", Optional.of(Price.of(105)), nbbo.bestAsk);
//		assertEquals("Incorrect BID", Optional.of(Price.of(109)), nbbo.bestBid);
//		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
//		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
//		assertEquals("Incorrect ASK market", market2, nbbo.bestAskMarket);
//		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
//	}
//	
//	@Test
//	public void transactionOrderingTest() throws IOException {
//		setup(Keys.NBBO_LATENCY, 0); // ZERO not IMMEDIATE
//		
//		Agent one = mockAgent(market1);
//		Agent two = mockAgent(market1);
//		
//		// First order
//		market1.submitOrder(one, BUY, Price.of(100), 1, NO_EXPIRATION);
//		market1.submitOrder(two, SELL, Price.of(100), 1, NO_EXPIRATION);
//		
//		// Second order
//		market1.submitOrder(two, BUY, Price.of(200), 2, NO_EXPIRATION);
//		market1.submitOrder(one, SELL, Price.of(200), 2, NO_EXPIRATION);
//		
//		// Hasn't propogated to transaction processor yet
//		assertTrue(Iterables.isEmpty(sip.getTransactions()));
//		
//		// Execute until proper time
//		sim.executeUntil(TimeStamp.ZERO);
//		// Now both transactions should show up
//		Iterator<Transaction> transactions = sip.getTransactions().iterator();
//		
//		// The real test is to check that they're in the proper order
//		Transaction first = transactions.next();
//		assertEquals(2, first.getQuantity());
//		assertEquals(Price.of(200), first.getPrice());
//		assertEquals(two, first.getBuyer());
//		assertEquals(one, first.getSeller());
//		
//		Transaction second = transactions.next();
//		assertEquals(1, second.getQuantity());
//		assertEquals(Price.of(100), second.getPrice());
//		assertEquals(one, second.getBuyer());
//		assertEquals(two, second.getSeller());
//		
//		assertFalse(transactions.hasNext());
//	}
//	
//	@Test
//	public void repeatTransactionOrderingTest() throws IOException {
//		for (int i = 0; i < 1000; ++i) {
//			setup();
//			transactionOrderingTest();
//		}
//	}
//	
//	@Test
//	public void transactionOrderingOtherMarketTest() throws IOException {
//		setup(Keys.NBBO_LATENCY, 0); // ZERO not IMMEDIATE
//		
//		Agent one = mockAgent(market1);
//		Agent two = mockAgent(market1);
//		
//		// First order
//		market1.submitOrder(one, BUY, Price.of(100), 1, NO_EXPIRATION);
//		market1.submitOrder(two, SELL, Price.of(100), 1, NO_EXPIRATION);
//		
//		// Second order
//		market1.submitOrder(two, BUY, Price.of(200), 2, NO_EXPIRATION);
//		market1.submitOrder(one, SELL, Price.of(200), 2, NO_EXPIRATION);
//
//		// Other
//		market2.submitOrder(two, BUY, Price.of(300), 3, NO_EXPIRATION);
//		market2.submitOrder(one, SELL, Price.of(300), 3, NO_EXPIRATION);
//
//		// Hasn't propogated to transaction processor yet
//		sim.executeUntil(TimeStamp.ZERO);
//		
//		// Execute until proper time
//		sim.executeUntil(TimeStamp.ZERO);
//		// Now both transactions should show up
//		
//		int index = 0, firstIndex = Integer.MAX_VALUE, secondIndex = Integer.MIN_VALUE;
//		for (Transaction trans : sip.getTransactions()) {
//			++index;
//			switch (trans.getQuantity()) {
//			case 1:
//				assertEquals(Price.of(100), trans.getPrice());
//				assertEquals(one, trans.getBuyer());
//				assertEquals(two, trans.getSeller());
//				firstIndex = index;
//				break;
//			case 2:
//				assertEquals(Price.of(200), trans.getPrice());
//				assertEquals(two, trans.getBuyer());
//				assertEquals(one, trans.getSeller());
//				secondIndex = index;
//				break;
//			case 3:
//				assertEquals(Price.of(300), trans.getPrice());
//				assertEquals(two, trans.getBuyer());
//				assertEquals(one, trans.getSeller());
//				break;
//			default:
//				fail("Too many");
//			}
//		}
//		
//		// Assert that the first order came in first. Don't care about the actual order
//		assertTrue(firstIndex < secondIndex);
//		// XXX This should be one of 1 2, 1 3, 2 3, but for some reason, I don't see 1 2. Not sure why
//	}
	
	private void setQuote(Market market, Price buy, Price sell) {
		setQuote(market, buy, 1, sell, 1);
	}
	
	private void setQuote(Market market, Price buy, int buyQuantity, Price sell, int sellQuantity) {
		Agent agent = mockAgent(market);
		market.getPrimaryView().submitOrder(agent, OrderRecord.create(market.getPrimaryView(), sim.getCurrentTime(), BUY, buy, buyQuantity));
		market.getPrimaryView().submitOrder(agent, OrderRecord.create(market.getPrimaryView(), sim.getCurrentTime(), SELL, sell, sellQuantity));
		sim.executeImmediate();
	}
	
	private Agent mockAgent(Market market) {
		return new BackgroundAgent(sim, market, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
		};
	}
	
}
