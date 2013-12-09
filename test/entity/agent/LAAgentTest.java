package entity.agent;

import static org.junit.Assert.*;
import static fourheap.Order.OrderType.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.Activity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import systemmanager.Consts;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

public class LAAgentTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market1, market2;
	private Agent agent1, agent2;
	private LAAgent la;
	private SIP sip;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "LAAgentTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market1 = new CDAMarket(sip, new Random(), new EntityProperties());
		market2 = new CDAMarket(sip, new Random(), new EntityProperties());
		agent1 = new MockAgent(fundamental, sip, market1);
		agent2 = new MockAgent(fundamental, sip, market2);
		la = new LAAgent(TimeStamp.IMMEDIATE,
				fundamental, sip, ImmutableList.of(market1, market2), new Random(), 1, 0.001);
	}
	
	/*
	 * Bug in LA that occurred in very particular circumstances. Imagine market1
	 * has BUY @ 30 and BUY @ 50. A SELL @ 10 arrives in market2. The LA submits
	 * a SELL @ 30 -> market1 and schedules a BUY @ 30 for market2. After the
	 * SELL clears, market1 has a BUY @ 30 left. There is still an arbitrage opp,
	 * and the LA acts again for before its second order goes into market2. So
	 * it submits a SELL @ 20 -> market1, and schedules a BUY @ 20 for market2.
	 * This first SELL clears, and results in no arbitrage opportunities, so
	 * then the first BUY @ 30 makes it market2 where it transacts. Finally the
	 * BUY @ 20 makes it to market2, but there are no more orders, and so this
	 * order sits while the LA holds a position.
	 * 
	 * FIXME this test still fails occasionally
	 */
	@Test
	public void oneSidedArbitrageTest() {
		/*
		 * This test doesn't seem very "unit," however it needs the market to
		 * clear and update the LAIP accordingly. This means we need execute all
		 * of the immediate events, and we need to use CDAMarkets instead of
		 * MockMarkets because we need them to clear after the original
		 * LAStrategy in order to trigger the next LAStrategy before the first
		 * has finished executing.
		 */
		executeImmediateActivities(market1.submitOrder(agent1, BUY, new Price(5), 1, TimeStamp.ZERO), TimeStamp.ZERO);
		executeImmediateActivities(market1.submitOrder(agent1, BUY, new Price(7), 1, TimeStamp.ZERO), TimeStamp.ZERO);
		executeImmediateActivities(market2.submitOrder(agent2, SELL, new Price(1), 1, TimeStamp.ZERO), TimeStamp.ZERO);
		
		executeImmediateActivities(la.agentStrategy(TimeStamp.ZERO), TimeStamp.ZERO);
		assertEquals(0, la.positionBalance);
	}

	private void executeImmediateActivities(Iterable<? extends Activity> acts, TimeStamp time) {
		ArrayList<Activity> queue = Lists.newArrayList(filterNonImmediateAndReverse(acts));
		while (!queue.isEmpty()) {
			Activity a = queue.get(queue.size() - 1);
			queue.remove(queue.size() - 1);
			queue.addAll(filterNonImmediateAndReverse(a.execute(time)));
		}
	}
	
	private Collection<? extends Activity> filterNonImmediateAndReverse(Iterable<? extends Activity> acts) {
		ArrayList<Activity> array = Lists.newArrayList();
		for (Activity a : acts)
			if (a.getTime() == TimeStamp.IMMEDIATE)
				array.add(a);
		Collections.reverse(array);
		return array;
	}

}
