package entity.agent;

import java.util.Arrays;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

import java.util.Map;

import static logger.Log.log;
import static logger.Log.Level.INFO;
import static logger.Log.Level.DEBUG;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * ADAPTIVEMARKETMAKER
 *
 * Meta Market Maker
 *
 * Based on Abernethy & Kale, "Adaptive Market Making via Online Learning", PNIPS 2013
 *
 * @author Benno Stein (bjs2@williams.edu)
 */
public class AdaptiveMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 4228181375500843232L;
	protected Map<Integer, Double> weights;
	protected final boolean useMedianSpread;
	protected final int volatilityBound;

	public AdaptiveMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			int tickSize, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickInside, int initLadderMean,
			int initLadderRange, int[] spreads, boolean useMedianSpread, int volatilityBound) {

		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				numRungs, rungSize, truncateLadder, tickImprovement, tickInside,
				initLadderMean, initLadderRange);

		this.useMedianSpread = useMedianSpread;
		this.volatilityBound = volatilityBound;

		//Initialize weights, mapping spread b-values to their corresponding weights, initially all equal.
		weights = Maps.newHashMapWithExpectedSize(spreads.length);
		double initial_weight = 1.0 / spreads.length;
		for (int i : spreads)
			{ weights.put(i, initial_weight); }
	}

	public AdaptiveMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, EntityProperties props) {

		this(scheduler, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.0005),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000),
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true),
				props.getAsBoolean(Keys.TICK_IMPROVEMENT, true),
				props.getAsBoolean(Keys.TICK_INSIDE, true),
				props.getAsInt(Keys.INITIAL_LADDER_MEAN, 0),
				props.getAsInt(Keys.INITIAL_LADDER_RANGE, 0),
				props.getAsIntArray(Keys.SPREADS, new int[]{200,400,800,1600,3200}),
				props.getAsBoolean(Keys.USE_MEDIAN_SPREAD, true),
				//To approximate volatility bound, use the fact that next = prev + kappa(mean-prev) + nextGaussian(0,1)*sqrt(shock)
				//conservatively estimate |mean-prev|<= 0.5mean; 98% confidence |nextGaussian| <= 2
				//so, delta ~= kappa * 1/2 * mean + 2sqrt(shock)
				(int) Math.round(0.5 * props.getAsDouble(Keys.FUNDAMENTAL_KAPPA) * props.getAsInt(Keys.FUNDAMENTAL_MEAN) + 2 * Math.sqrt(props.getAsInt(Keys.FUNDAMENTAL_SHOCK_VAR)))
			);
	}

	/**
	 * @return a spread value: either the median or one chosen at random according to the current weights, depending on "useMedianSpread" parameter
	 */
	protected int getSpread(){
		double r = useMedianSpread ? 0.5 : rand.nextDouble();
		double p_sum = 0.0;
		Integer[] spreads = (Integer[]) (weights.keySet().toArray());
		Arrays.sort(spreads);
		for(Integer spread : spreads){
			p_sum += weights.get(spread);
			if (p_sum >= r) { return spread; }
		}
		// This return will only be reached if r is very(!) close to 1 and rounding errors make the sum of the weights less than 1. Extremely unlikely. 
		return 0;
	}
	
	/**
	 * Given a spread, computes the transactions that would have been made during the last time step
	 * had that spread been chosen.
	 *
	 * @param spread The spread value for which to calculate transactions from the last time step
	 * @param bid The current bid price.
	 * @param ask The current ask price.
	 *
	 * @return a TransactionResult: a pair of integers (netHoldingsChange, netCashChange)
	 */
	protected TransactionResult lastTransactionResult(int spread, int bidPrice, int askPrice){
		int delta_h = 0, delta_c = 0; //changes in holdings / cash, respectively
		int lastBidPrice = lastBid.intValue(), lastAskPrice = lastAsk.intValue();
		for(int rung = 0; rung < numRungs; rung++){
			int offset = spread + rung * stepSize;
			if(lastBidPrice - offset >= askPrice) 		//If this bid would have transacted
				{ delta_h += 1; delta_c -= (lastBid.intValue() - offset);}
			if(lastAskPrice + offset <= bidPrice) 		//If this ask would have transacted
				{ delta_h -= 1; delta_c += (lastAsk.intValue() + offset);}
		}
		return new TransactionResult(delta_h, delta_c);
	}


	/**
	 * Recalculate weights based on their hypothetical performance in the last timestep
	 * In this case, use Multiplicative Weights.  Can be subclassed and overridden to implement other learning algorithms.
	 * @param valueDeltas: a mapping of spread values to the net change in their portfolio value over the last time step
	 * @param currentTime
	 */
	protected void recalculateWeights(Map<Integer, Integer> valueDeltas, TimeStamp currentTime){

		int maxSpread = 0;
		for(int spread : weights.keySet()){
			maxSpread = Math.max( maxSpread, spread * 2 );
		}
		//Compute G as per Abernethy & Kale Lemma 4
		int G = 2 * volatilityBound * maxSpread + volatilityBound * volatilityBound;
		double eta_t = Math.min(Math.sqrt(Math.log(weights.size())/currentTime.getInTicks()), 1.0) / (2 * G);

		for(Map.Entry<Integer,Double> e : weights.entrySet()){
			e.setValue(e.getValue() * Math.exp(eta_t * valueDeltas.get(e.getKey())));
		}
		normalizeWeights();
	}

	/**
	 * Adjusts all weights by a constant factor s.t. their sum is 1.
	 */
	protected void normalizeWeights(){
		double total = 0;
		for(double w : weights.values()) { total += w; }
		for(int s : weights.keySet()) { weights.put(s, weights.get(s) / total); }
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		super.agentStrategy(currentTime);

		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();

		//Approximate the price of a single unit as the midpoint between the quoted bid and ask prices
		int approximateUnitPrice = (bid.intValue() + ask.intValue()) / 2;

		//For each spread, determine how it would have performed in the last round.
		ImmutableMap.Builder<Integer,Integer> builder = new ImmutableMap.Builder<Integer,Integer>();
		for(int spread : weights.keySet()){
			TransactionResult transaction = lastTransactionResult(spread, bid.intValue(), ask.intValue());
			builder.put(spread, transaction.getNetCashChange() + (approximateUnitPrice * transaction.getNetHoldingsChange()));//<change_in_cash> + <change_in_holdings>*<approximate_unit_value>
		}
		ImmutableMap<Integer,Integer> valueDeltas = builder.build();

		//Recalculate weights based on performances from the last timestep, using Multiplicative Weights (Abernethy&Kale 4.1)
		recalculateWeights(valueDeltas, currentTime);

		log.log(DEBUG, "%s in %s: Current spread weights: %s", 
				this, primaryMarket, weights.toString());

		//Submit updated order ladder, using the spread chosen by the learning algorithm
		int spread = getSpread();
		int ladderSize = stepSize * numRungs;
		withdrawAllOrders();
		submitOrderLadder(new Price(approximateUnitPrice - spread - ladderSize),  //minimum buy
						  new Price(approximateUnitPrice - spread), 			  //maximum buy
						  new Price(approximateUnitPrice + spread),				  //minimum sell
						  new Price(approximateUnitPrice + spread + ladderSize)); //maximum sell
		log.log(INFO, "%s in %s: submitting ladder with spread %d",
				this, primaryMarket, spread);
	}

	protected static class TransactionResult extends utils.Pair<Integer, Integer>{
		protected TransactionResult(Integer netHoldingsChange, Integer netCashChange)
			{ super(netHoldingsChange, netCashChange); }
		public Integer getNetHoldingsChange() { return this.left;  }
		public Integer getNetCashChange()	  { return this.right; }
	}

}
