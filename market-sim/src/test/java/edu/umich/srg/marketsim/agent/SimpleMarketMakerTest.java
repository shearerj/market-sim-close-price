package edu.umich.srg.marketsim.agent;

import static org.junit.Assert.assertTrue;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.NumRungs;
import edu.umich.srg.marketsim.Keys.RungSep;
import edu.umich.srg.marketsim.Keys.RungThickness;
import edu.umich.srg.marketsim.Keys.TickImprovement;
import edu.umich.srg.marketsim.Keys.TickOutside;
import edu.umich.srg.marketsim.MarketSimulator;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleMarketMakerTest {

  private static final Random rand = new Random();

  @Test
  public void basicIntegrationTransactionTest() {
    long length = 20;
    Fundamental fundamental = ConstantFundamental.create(500, length);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));

    // Background Agents
    for (int i = 0; i < 20; ++i)
      sim.addAgent(new NoiseAgent(sim, cda, Spec.fromPairs(ArrivalRate.class, 0.5), rand));

    // Market Maker
    AtomicBoolean transacted = new AtomicBoolean(false);
    cda.addTransactionObserver((price, quant) -> {
      transacted.set(true);
    });

    SimpleMarketMaker marketMaker = new SimpleMarketMaker(sim, cda,
        Spec.builder().put(ArrivalRate.class, 0.5).put(RungThickness.class, 1)
            .put(NumRungs.class, 4).put(RungSep.class, 10).put(TickImprovement.class, false)
            .put(TickOutside.class, false).build(),
        rand);
    sim.addAgent(marketMaker);

    // Run
    sim.initialize();
    sim.executeUntil(TimeStamp.of(length));

    assertTrue("MarketMaker didn't transact", transacted.get());
  }

}
