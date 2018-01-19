package edu.umich.srg.marketsim.observer;

import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.market.MarketObserver.QuoteObserver;
import edu.umich.srg.marketsim.market.Quote;

public class WaitForQuoteObserver implements QuoteObserver {

  private final Sim sim;
  private final Runnable action;
  private boolean waiting;
  private Quote quote;

  private WaitForQuoteObserver(Sim sim, Runnable action) {
    this.sim = sim;
    this.action = action;
    this.waiting = false;
    this.quote = Quote.empty();
  }

  @Override
  public void notifyQuote(Quote quote) {
    this.quote = quote;
    if (waiting) {
      waiting = false;
      sim.scheduleIn(TimeStamp.ZERO, action);
    }
  }

  public void beginWaiting() {
    waiting = true;
  }

  public Quote getQuote() {
    return quote;
  }

  public static WaitForQuoteObserver create(Sim sim, Runnable action) {
    return new WaitForQuoteObserver(sim, action);
  }

}
