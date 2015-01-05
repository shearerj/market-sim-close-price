package entity.agent;

import static logger.Log.Level.INFO;
import logger.Log;
import systemmanager.Keys.FundEstimate;
import systemmanager.Keys.Spread;
import utils.Rand;

import com.google.common.base.Optional;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.strategy.FinalFundamentalEstimator;
import entity.market.Market;
import entity.market.Price;
import entity.sip.MarketInfo;
import event.Timeline;

/**
 * FundamentalMM
 * 
 * Quotes a spread fixed around an estimate of the fundamental.
 * 
 * Estimate can be based on a fixed input argument (fundEstimate)
 * but if this value is not specified, then it uses the r_hat estimation
 * function (same as what's used by ZIRPs) to estimate the fundamental value.
 * 
 * MM will lose time priority if use last bid & ask, so use tick improvement
 * and quote inside the spread.
 * 
 * @author ewah
 */
public class FundamentalMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 9057600979711100221L;
	
	private final Price fundamentalEstimate;
	private final Price constSpread;
	private final FinalFundamentalEstimator fundamental;
	
	private Optional<Price> lastBid, lastAsk;

	public FundamentalMarketMaker(int id, Stats stats, Timeline timeline, Log log, Rand rand, 
			MarketInfo sip, FundamentalValue fundamental, Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, market, props);
		
		this.fundamental = FinalFundamentalEstimator.create(getFundamentalValueView(), timeline, props);
		this.fundamentalEstimate = props.get(FundEstimate.class);
		this.constSpread = props.get(Spread.class);
		
		this.lastBid = Optional.absent();
		this.lastAsk = Optional.absent();
	}

	public static FundamentalMarketMaker create(int id, Stats stats, Timeline timeline, Log log, 
			Rand rand, MarketInfo sip, FundamentalValue fundamental, Market market, Props props) {
		return new FundamentalMarketMaker(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}

	@Override
	protected void agentStrategy() {
		super.agentStrategy();

		Optional<Price> bid = getQuote().getBidPrice();
		Optional<Price> ask = getQuote().getAskPrice();

		log(INFO, "%s in %s: Withdraw all orders.", this, primaryMarket);
		withdrawAllOrders();

		bid = getQuote().getBidPrice();
		ask = getQuote().getAskPrice();

		// Use last known bid/ask if undefined post-withdrawal
		if (!this.getQuote().isDefined()) {
			Optional<Price> oldBid = bid, oldAsk = ask;
			bid = bid.or(lastBid);
			ask = ask.or(lastAsk);
			log(INFO, "%s in %s: Ladder MID (%s, %s)-->(%s, %s)", 
					this, primaryMarket, oldBid, oldAsk, bid, ask);
		}
		int offset = this.initLadderRange / 2;
		if (bid.isPresent() && ask.isPresent()) { 
			offset = (ask.get().intValue() - bid.get().intValue()) / 2;
		}
		if (!constSpread.equals(Price.ZERO)) {
			offset = this.constSpread.intValue() / 2;
		}
		Price fundamentalPrice = fundamentalEstimate.equals(Price.ZERO) ? fundamental.getFundamentalEstimate() : fundamentalEstimate;
		log(INFO, "%s in %s: Spread of %s around estimated fundamental %s, ladderBid=%s, ladderAsk=%s", 
				this, primaryMarket, Price.of(offset), fundamentalPrice,
				Price.of(fundamentalPrice.intValue() - offset),
				Price.of(fundamentalPrice.intValue() + offset));
		createOrderLadder(Optional.of(Price.of(fundamentalPrice.intValue() - offset)),
				Optional.of(Price.of(fundamentalPrice.intValue() + offset)));
		
		// FIXME is this the right thing to do
		lastBid = bid;
		lastAsk = ask;
	}
}
