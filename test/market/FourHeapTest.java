package market;

import model.*;
import entity.*;
import event.TimeStamp;

import org.junit.Before;
import org.junit.Test;


public class FourHeapTest {

	private MarketModel model;
	private Market market;
	private FourHeap FH;
	private int idx;
	
	@Before
	public void setup() {
		model = new MockMarketModel(1);
		market = new CDAMarket(1, model, 0); // TODO Dummy IP
		FH = new FourHeap(market);
		idx = 1;
	}
	
	@Test
	public void InsertSingleBid() {
		MockAgent agent1 = new MockAgent(idx++, model, market);
		TimeStamp time1 = new TimeStamp(0);
		PQBid pq1 = new PQBid(agent1, market, time1);
		PQPoint p1 = new PQPoint(-1, new Price(150), pq1);
		FH.insertBid(p1);
		printFH();
	}
	
	@Test
	public void InsertTwoBid() {
		MockAgent agent1 = new MockAgent(idx++, model, market);
		MockAgent agent2 = new MockAgent(idx++, model, market);
		TimeStamp time1 = new TimeStamp(0);
		PQBid pq1 = new PQBid(agent1, market, time1);
		PQPoint p1 = new PQPoint(-1, new Price(150), pq1);
		PQBid pq2 = new PQBid(agent2, market, time1);
		PQPoint p2 = new PQPoint(-1, new Price(100), pq2);
		FH.insertBid(p1);
		//printFH();
		FH.insertBid(p2);
		printFH();
	}

	public void printFH() {
		String s =  FH.printSet(0) + FH.printSet(1) + FH.printSet(2) + FH.printSet(3);
		System.out.println(s);
	}
	
	@Test
	public void ExtraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			InsertTwoBid();
		}
	}
}
