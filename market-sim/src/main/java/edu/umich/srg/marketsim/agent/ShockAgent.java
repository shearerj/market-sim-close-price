package edu.umich.srg.marketsim.agent;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;

import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.NumShockOrders;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Type;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.market.Quote;

import java.util.Collection;
import java.util.Random;
import java.util.function.Predicate;

// TODO This could be made more efficient if quote gave access to number, or if market gave depth.

public class ShockAgent implements Agent {

  private final Sim sim;
  private final MarketView market;
  private final OrderType type;
  private final Price orderPrice;
  private final TimeStamp arrivalTime;
  private final Predicate<Quote> submitFunction;

  private int ordersToSubmit;
  private boolean waitingToSubmit;

  public ShockAgent(Sim sim, Market market, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.arrivalTime =
        TimeStamp.of(spec.get(SimLength.class) / 2);
    this.type = spec.get(Type.class);
    this.orderPrice = type == BUY ? Price.INF : Price.ZERO;
    this.submitFunction =
        type == BUY ? (q -> q.getAskPrice().isPresent()) : (q -> q.getBidPrice().isPresent());
    this.ordersToSubmit = spec.get(NumShockOrders.class);
    this.waitingToSubmit = false;
  }

  public static ShockAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new ShockAgent(sim, market, spec, rand);
  }

  private void strategy() {
    if (ordersToSubmit > 0 && submitFunction.test(market.getQuote())) {
      market.submitOrder(type, orderPrice, 1);
      --ordersToSubmit;
      waitingToSubmit = false;
    } else if (ordersToSubmit > 0) {
      waitingToSubmit = true;
    }
  }

  @Override
  public void initilaize() {
    sim.scheduleIn(arrivalTime, this::strategy);
  }

  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  @Override
  public JsonObject getFeatures() {
    return new JsonObject();
  }

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {}

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    waitingToSubmit = true;
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {
    if (waitingToSubmit)
      strategy();
  }

  @Override
  public void notifyTransaction(MarketView market, Price price, int quantity) {}

}
