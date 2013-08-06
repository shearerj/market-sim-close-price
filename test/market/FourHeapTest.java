package market;

import model.*;
import entity.*;
import entity.market.Bid;
import entity.market.CDAMarket;
import entity.market.FourHeap;
import entity.market.Market;
import entity.market.Point;
import entity.market.Price;
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
		Bid pq1 = new Bid(agent1, market, time1);
		Point p1 = new Point(-1, new Price(150), pq1);
		FH.insertBid(p1);
		printFH();
	}
	
	@Test
	public void InsertTwoBid() {
		MockAgent agent1 = new MockAgent(idx++, model, market);
		MockAgent agent2 = new MockAgent(idx++, model, market);
		TimeStamp time1 = new TimeStamp(0);
		Bid pq1 = new Bid(agent1, market, time1);
		Point p1 = new Point(-1, new Price(150), pq1);
		Bid pq2 = new Bid(agent2, market, time1);
		Point p2 = new Point(-1, new Price(100), pq2);
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
