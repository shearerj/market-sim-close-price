package edu.umich.srg.marketsim.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.MarketObserver.QuoteObserver;
import edu.umich.srg.marketsim.testing.MockSim;

import org.junit.Test;

public class GetQuoteObserverTest {

  @Test
  public void cacheWorks() {
    Sim sim = new MockSim();
    Fundamental fund = ConstantFundamental.create(0, 100);
    Market market1 = CdaMarket.create(sim, fund);
    Market market2 = CdaMarket.create(sim, fund);

    QuoteObserver observer11 = GetQuoteObserver.create(market1);
    QuoteObserver observer21 = GetQuoteObserver.create(market2);
    QuoteObserver observer12 = GetQuoteObserver.create(market1);
    QuoteObserver observer22 = GetQuoteObserver.create(market2);

    assertEquals(observer11, observer12);
    assertEquals(observer21, observer22);
    assertNotEquals(observer11, observer21);
  }

}
