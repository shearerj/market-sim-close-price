package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

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
	
	protected Price fundamentalEstimate;
	protected int simulationLength;
	protected double fundamentalKappa;
	protected double fundamentalMean;

	public FundamentalMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			int tickSize, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickOutside, int initLadderMean, 
			int initLadderRange, int simLength, double kappa, double fundamentalMean,
			int fundamentalEstimate) {

		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				numRungs, rungSize, truncateLadder, tickImprovement, tickOutside,
				initLadderMean, initLadderRange);
		
		this.fundamentalEstimate = (fundamentalEstimate > 0) ? new Price(fundamentalEstimate) : null;
		simulationLength = simLength;
		fundamentalKappa = kappa;
		this.fundamentalMean = fundamentalMean;
	}

	public FundamentalMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, EntityProperties props) {

		this(scheduler, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.0005),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000),
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true),
				props.getAsBoolean(Keys.TICK_IMPROVEMENT, true),
				props.getAsBoolean(Keys.TICK_OUTSIDE, false),	// always submit inside the quote unless otherwise specified
				props.getAsInt(Keys.INITIAL_LADDER_MEAN, props.getAsInt(Keys.FUNDAMENTAL_MEAN)),
				props.getAsInt(Keys.INITIAL_LADDER_RANGE, 5000),
				props.getAsInt(Keys.SIMULATION_LENGTH),	// no default needed, already going to be in the EntityProperties file
				props.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
				props.getAsInt(Keys.FUNDAMENTAL_MEAN),
				props.getAsInt(Keys.FUNDAMENTAL_ESTIMATE, -1));
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		super.agentStrategy(currentTime);

		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();

		if (bid == null && lastBid == null && ask == null && lastAsk == null) {
			log.log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
			this.createOrderLadder(bid, ask);	
			
		} else if ((bid == null && lastBid != null)
				|| (bid != null && !bid.equals(lastBid))
				|| (bid != null && lastBid == null)
				|| (ask == null && lastAsk != null)
				|| (ask != null && !ask.equals(lastAsk))
				|| (ask != null && lastAsk == null)) {

			if (!this.getQuote().isDefined()) {
				log.log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
				this.createOrderLadder(bid, ask);
				
			} else {
				// Quote changed, still valid, withdraw all orders
				log.log(INFO, "%s in %s: Withdraw all orders.", this, primaryMarket);
				withdrawAllOrders();
				
				bid = this.getQuote().getBidPrice();
				ask = this.getQuote().getAskPrice();
				
				// Use last known bid/ask if undefined post-withdrawal
				if (!this.getQuote().isDefined()) {
					Price oldBid = bid, oldAsk = ask;
					if (bid == null && lastBid != null) bid = lastBid;
					if (ask == null && lastAsk != null) ask = lastAsk;
					log.log(INFO, "%s in %s: Ladder MID (%s, %s)-->(%s, %s)", 
							this, primaryMarket, oldBid, oldAsk, bid, ask);
				}
				int offset = (ask.intValue() - bid.intValue()) / 2;
				
				if (fundamentalEstimate == null) {
					fundamentalEstimate = this.getEstimatedFundamental(currentTime, simulationLength, 
							fundamentalKappa, fundamentalMean);
				}
				this.createOrderLadder(new Price(fundamentalEstimate.intValue() - offset),
										new Price(fundamentalEstimate.intValue() + offset));
			}
			
		} else {
			log.log(INFO, "%s in %s: No change in submitted ladder", this, primaryMarket);
		}
		// update latest bid/ask prices
		lastAsk = ask; lastBid = bid;
	}

}
