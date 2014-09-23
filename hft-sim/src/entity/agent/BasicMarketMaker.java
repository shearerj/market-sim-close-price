package entity.agent;

import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Simulation;

import com.google.common.base.Optional;

import data.Props;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;

/**
 * BASICMARKETMAKER
 * 
 * Basic market maker. See description in (Chakraborty & Kearns, 2011).
 * Participates in only a single market at a time. Submits a ladder of bids
 * based on BID = Y_t < X_t = ASK, where C_t = numRungs * stepSize:
 * 
 * buy orders:  [Y_t - C_t, ..., Y_t - C_1, Y_t]
 * sell orders: [X_t, X_t + C_1, ..., X_t + C_t]
 * 
 * The market maker liquidates its position at the price dictated by the
 * global fundamental at the end of the simulation.
 * 
 * The market maker will only submit a ladder if both the bid and the ask
 * are defined.
 * 
 * If, after it withdraws its orders, it notices that the quote is now 
 * undefined, it will submit a ladder with either lastBid or lastAsk (or
 * both) in lieu of the missing quote component. Notice that if
 * lastBid/Ask crosses the current ASK/BID, truncation will handle this.
 * 
 * MM will lose time priority if use last bid & ask, so use tick improvement
 * and quote inside the spread.
 * 
 * @author ewah
 */
public class BasicMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 9057600979711100221L;
	
	protected Optional<Price> lastAsk, lastBid; // stores the ask/bid at last entry

	protected BasicMarketMaker(Simulation sim, Market market, Random rand, Props props) {
		super(sim, market, rand, props);

		this.lastAsk = Optional.absent();
		this.lastBid = Optional.absent();
	}

	public static BasicMarketMaker create(Simulation sim, Market market, Random rand, Props props) {
		return new BasicMarketMaker(sim, market, rand, props);
	}

	@Override
	public void agentStrategy() {
		super.agentStrategy();

		Quote quote = getQuote();
		Optional<Price> bid = quote.getBidPrice();
		Optional<Price> ask = quote.getAskPrice();

		if (!bid.isPresent() && !lastBid.isPresent() && !ask.isPresent() && !lastAsk.isPresent()) {
			createOrderLadder(bid, ask);
			
		} else if (bid.equals(lastBid) && ask.equals(lastAsk)) {
			log(INFO, "%s in %s: No change in submitted ladder", this, primaryMarket);
			
		} else {
			if (!getQuote().isDefined()) {
				log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
				createOrderLadder(bid, ask);
				
			} else {
				// Quote changed, still valid, withdraw all orders
				log(INFO, "%s in %s: Withdraw all orders.", this, primaryMarket);
				withdrawAllOrders();
				
				// XXX This will only update if market maker is instantaneous
				bid = getQuote().getBidPrice();
				ask = getQuote().getAskPrice();
				
				// Use last known bid/ask if undefined post-withdrawal
				if (!getQuote().isDefined()) {
					Optional<Price> oldBid = bid, oldAsk = ask;
					bid = bid.or(lastBid);
					ask = bid.or(lastAsk);
					log(INFO, "%s in %s: Ladder MID (%s, %s)-->(%s, %s)", 
							this, primaryMarket, oldBid, oldAsk, bid, ask);
				}
				
				submitCalculatedSpread(bid, ask);
			}
		}
		lastBid = bid;
		lastAsk = ask;
	}
	
	/** method to calculate spread when quote is definied */
	protected void submitCalculatedSpread(Optional<Price> bid, Optional<Price> ask) {
		createOrderLadder(bid, ask);
	}
}
