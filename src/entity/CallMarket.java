package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import logger.*;
import market.Bid;
import market.PQBid;
import market.Price;
import market.Quote;
import model.MarketModel;
import systemmanager.Consts;
import activity.Activity;
import activity.Clear;
import event.TimeStamp;

/**
 * Class for a call market. The order book is closed, therefore agents will only
 * be able to see the price of the last clear as well as the bid/ask immediately
 * after the clear, i.e. they will be able to see the best available buy and
 * sell prices for the bids left in the order book after each market clear.
 * 
 * NOTE: First Clear Activity is initialized in the SystemManager.
 * 
 * @author ewah
 */
public class CallMarket extends Market {

	public final static String CLEAR_FREQ_KEY = "clearFreq";
	public final static String PRICING_POLICY_KEY = "pricingPolicy";

	public float pricingPolicy; // XXX Unused?
	protected final TimeStamp clearFreq;
	protected TimeStamp nextClearTime;

	public CallMarket(int marketID, MarketModel model, SIP sip,
			float pricingPolicy, TimeStamp clearFreq) {
		super(marketID, model, sip);

		if (clearFreq.after(Consts.START_TIME))
			throw new IllegalArgumentException(
					"Can't create a call market with 0 clear frequency. Create a CDA instead.");

		this.pricingPolicy = pricingPolicy;
		this.clearFreq = clearFreq;
		this.nextClearTime = clearFreq;
	}

	@Override
	public Bid getBidQuote() {
		return this.orderbook.getBidQuote();
	}

	@Override
	public Bid getAskQuote() {
		return this.orderbook.getAskQuote();
	}

	@Override
	public Price getBidPrice() {
		return lastBidPrice;
	}

	@Override
	public Price getAskPrice() {
		return lastAskPrice;
	}

	@Override
	public Collection<? extends Activity> addBid(Bid bid, TimeStamp currentTime) {
		orderbook.insertBid((PQBid) bid); // FIXME This is bad. Add bid should
											// enforce PQBid if this is the
											// case.
		bids.add(bid);
		addDepth(currentTime, orderbook.getDepth());
		return Collections.emptySet();
	}

	@Override
	public Collection<? extends Activity> removeBid(Agent agent, TimeStamp ts) {
		orderbook.removeBid(agent.getID());
		addDepth(ts, orderbook.getDepth());
		return Collections.emptySet();
	}

	@Override
	public Map<Integer, Bid> getBids() {
		return orderbook.getActiveBids();
	}

	@Override
	public Collection<? extends Activity> clear(TimeStamp currentTime) {
		// Update the next clear time
		nextClearTime = currentTime.plus(clearFreq);
		Collection<Activity> activities = new ArrayList<Activity>(
				super.clear(currentTime));

		// Insert next clear activity at some time in the future
		activities.add(new Clear(this, nextClearTime));
		return activities;
	}

	public Quote quote(TimeStamp quoteTime) {
		// updates market's quote only immediately after a Clear activity
		// otherwise revises the given quote to be the last known prices

		Quote q = new Quote(this);

		// Retrieve the newest quote if quote is requested right after clearing
		if (quoteTime.compareTo(lastClearTime) == 0) {
			Price bp = q.lastBidPrice;
			Price ap = q.lastAskPrice;

			if (bp != null && ap != null) {
				if (bp.getPrice() == -1 || ap.getPrice() == -1) {
					// either bid or ask are undefined
					this.addSpread(quoteTime, Price.INF.getPrice());
					this.addMidQuote(quoteTime, Price.INF,
							Price.INF);

				} else if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
					Logger.log(Logger.Level.ERROR, this.getName()
							+ "::quote: ERROR bid > ask");
					this.addSpread(quoteTime, Price.INF.getPrice());
					this.addMidQuote(quoteTime, Price.INF,
							Price.INF);
				} else {
					// valid bid-ask
					data.addQuote(id, q);
					this.addSpread(quoteTime, q.getSpread());
					this.addMidQuote(quoteTime, bp, ap);
				}
			}
			lastQuoteTime = quoteTime;
			nextQuoteTime = quoteTime.plus(clearFreq);

			if (bp != null) {
				lastBidPrice = bp;
				lastBidQuantity = q.lastBidQuantity;
			}
			if (ap != null) {
				lastAskPrice = ap;
				lastAskQuantity = q.lastAskQuantity;
			}

		} else {
			// Otherwise, retrieve the last known quote & overwrite current
			// quote
			q.lastBidPrice = lastBidPrice;
			q.lastAskPrice = lastAskPrice;
			q.lastBidQuantity = lastBidQuantity;
			q.lastAskQuantity = lastAskQuantity;
		}

		return q;
	}
}
