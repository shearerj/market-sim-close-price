package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Log.Level.INFO;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import systemmanager.Keys.FastLearning;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.MovingAveragePrice;
import systemmanager.Keys.NumHistorical;
import systemmanager.Keys.Spreads;
import systemmanager.Keys.UseLastPrice;
import systemmanager.Keys.UseMedianSpread;
import systemmanager.Simulation;

import com.google.common.base.Optional;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.math.DoubleMath;

import data.Props;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;

/**
 * ADAPTIVEMARKETMAKER
 *
 * Adaptive Market Maker
 *
 * Based on Abernethy & Kale, "Adaptive Market Making via Online Learning", PNIPS 2013
 *
 * @author Benno Stein (bjs2@williams.edu)
 */
public class AdaptiveMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 4228181375500843232L;

	protected Map<Integer, Double> weights;			// key = spread, value = weight
	protected final boolean useMedianSpread;
	protected final int volatilityBound;			// Delta in paper
	protected EvictingQueue<Integer> priceQueue;
	protected Optional<Price> lastPrice;			// track price in previous reentry (period)
	protected int numReentries = 0;					// tracks num reentries
	protected final boolean useLastPrice;			// use prices from last period o/w use current reentry prices // XXX to maintain compatibility with previous version
	protected final boolean fastLearning;			// XXX to maintain compatibility with previous version
	
	protected Optional<Price> lastAsk, lastBid; // stores the ask/bid at last entry

	protected AdaptiveMarketMaker(Simulation sim, Market market, Random rand, Props props) {
		super(sim, market, rand, props);

		boolean movingAveragePrice = props.get(MovingAveragePrice.class);
		int numHistorical = movingAveragePrice ? 8 : props.get(NumHistorical.class);
		checkArgument(numHistorical > 0, "Number of historical prices must be positive!");
		this.priceQueue = EvictingQueue.create(numHistorical);
		
		this.fastLearning = props.get(FastLearning.class);
		this.useLastPrice = props.get(UseLastPrice.class);
		this.useMedianSpread = props.get(UseMedianSpread.class);
		/*
		 * To approximate volatility bound, use the fact that next = prev +
		 * kappa(mean-prev) + nextGaussian(0,1)*sqrt(shock) conservatively
		 * estimate |mean-prev|<= 0.25*mean; 98% confidence |nextGaussian| <= 2
		 * so, delta ~= kappa * 0.25 * mean + 2sqrt(shock)
		 */
		this.volatilityBound = DoubleMath.roundToInt(
				0.25
				* props.get(FundamentalKappa.class)
				* props.get(FundamentalMean.class) + 2
				* Math.sqrt(props.get(FundamentalShockVar.class)),
				RoundingMode.HALF_EVEN);

		// FIXME Move to expert class / interface
		// Initialize weights, initially all equal = 1/N, where N = # windows
		// spreads = windows in paper, variable b
		List<Integer> spreads = ImmutableList.copyOf(props.get(Spreads.class));
		this.weights = Maps.newTreeMap(); // XXX Tree map to support easy median implementation
		double initial_weight = 1.0 / spreads.size();
		for (int i : spreads)
			weights.put(i, initial_weight);
		
		this.lastPrice = Optional.absent();
		this.lastAsk = Optional.absent();
		this.lastBid = Optional.absent();
	}
	
	public static AdaptiveMarketMaker create(Simulation sim, Market market, Random rand, Props props) {
		return new AdaptiveMarketMaker(sim, market, rand, props);
	}

	/**
	 * Returns the spread to be used by the MM. If "useMedianSpread" parameter
	 * is true, then uses the median spread (based on distribution of weights). 
	 * Otherwise uses one at random.
	 *
	 * @return a spread value: either the median or one chosen at random.
	 */
	protected int getSpread(){
		double r = useMedianSpread ? 0.5 : rand.nextDouble();
		double sum = 0.0;

		for (Entry<Integer, Double> e : weights.entrySet()) {
			sum += e.getValue();
			if (sum >= r)
				return e.getKey();
		}
		// Rounding errors caused it to get here... FIXME Should just verify this can't happen
		throw new IllegalStateException("Sum of weights was less than one");
	}

	/**
	 * Given a spread, determines the transactions that would have executed 
	 * during the last time step had that spread been chosen.
	 *
	 * @param spread 	spread to use for result computation
	 * @param bid 		The current bid price.
	 * @param ask 		The current ask price.
	 *
	 * @return TransactionResult: a pair of integers (holdingsChange, cashChange)
	 */
	protected TransactionResult lastTransactionResult(int spread, Price bid, Price ask){
		int delta_h = 0, delta_c = 0; //changes in holdings / cash, respectively
		if (lastBid.isPresent() && lastAsk.isPresent() && lastPrice.isPresent()) {
			for(int rung = 0; rung < numRungs; rung++) {
				int offset = spread/2 + rung * stepSize;
				// Step through prices in ladder
				if (lastPrice.get().intValue() - offset >= ask.intValue()) {
					//If this buy order would have transacted
					delta_h++; 
					delta_c -= (lastPrice.get().intValue() - offset);
				}
				if (lastPrice.get().intValue() + offset <= bid.intValue()) {
					//If this sell order would have transacted
					delta_h--;
					delta_c += (lastPrice.get().intValue() + offset);
				}
			}
		}
		/*If lastBid or lastAsk is null, just return the default delta_h=delta_c=0 so weights don't readjust */
		return new TransactionResult(delta_h, delta_c);
	}

	/**
	 * Recalculate weights based on their hypothetical performance in the last timestep
	 * In this case, use Multiplicative Weights.  Can be subclassed and overridden 
	 * to implement other learning algorithms.
	 * 
	 * In round t, updates using rule:
	 * 		w_next(b) = w_curr(b) * exp(eta_t * V_next(b) - V_curr(b))
	 * with w_next normalized. 
	 * 
	 * eta_t is set to bound the algorithm's regret by 13G sqrt(log(N) * T)
	 * 		G = 2B * volatilityBound + volatilityBound^2
	 * 		B = maximum window size/spread given.
	 * 
	 * XXX Use G = delta / 5 rather than G = delta*B*2 + delta^2 to have agent
	 * 		learn more aggressively/quickly
	 * 
	 * @param valueDeltas: a mapping of spread values to the net change in their 
	 * 		portfolio value over the last time step
	 * @param currentTime
	 */
	protected void recalculateWeights(Map<Integer, Integer> valueDeltas){
		int maxSpread = 0;						// B = upper bound of spread sizes
		for(int spread : weights.keySet()) {
			maxSpread = Math.max( maxSpread, spread );
		}
		
		double G = 2 * maxSpread * volatilityBound + volatilityBound^2; 	
		if (fastLearning) {	// XXX just to make this backwards compatible
			G = (int) (volatilityBound / 5);
		}
		
		double eta = Math.min( Math.sqrt( Math.log(weights.size()) / numReentries ), 1.0) / (2 * G);
		
		for(Map.Entry<Integer,Double> e : weights.entrySet())
			e.setValue(e.getValue() * Math.exp(eta * valueDeltas.get(e.getKey())));
		
		normalizeWeights();
	}

	/**
	 * Adjusts all weights by a constant factor s.t. their sum is 1.
	 */
	protected void normalizeWeights(){
		double total = 0;
		for (double w : weights.values())
			total += w;
		for (Map.Entry<Integer, Double> e : weights.entrySet())
			e.setValue(e.getValue() / total);
	}

	@Override
	protected void agentStrategy() {
		super.agentStrategy();
		numReentries++;

		Quote quote = getQuote();
		Optional<Price> bid = quote.getBidPrice();
		Optional<Price> ask = quote.getAskPrice();
		
		// if no orders in the market yet
		if (!quote.isDefined()) {
			log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
			createOrderLadder(bid, ask);
			
		} else {
			// Approximate the price as the midquote price
			int midQuotePrice = (bid.get().intValue() + ask.get().intValue()) / 2;
			priceQueue.add(midQuotePrice);

			double sumPrices = 0;
			for (int x : priceQueue) sumPrices += x;
			Price avgPrice = Price.of(sumPrices / priceQueue.size());

			//For each spread, determine how it would have performed in the last round. (XXX current round?)
			ImmutableMap.Builder<Integer,Integer> value = new ImmutableMap.Builder<Integer,Integer>();
			for(int spread : weights.keySet()){
				TransactionResult result = lastTransactionResult(spread, bid.get(), ask.get());
				// Value(t+1) = Cash(t+1) + price(t) * Holdings(t+1)

				if (!useLastPrice) {	// XXX for compatibility purposes
					value.put(spread, result.getCashChange() + (avgPrice.intValue() * result.getHoldingsChange()));
				} else {
					// XXX new version using last average price, not current (evaluate for last period)
					if (lastPrice.isPresent())
						value.put(spread, result.getCashChange() + (lastPrice.get().intValue() * result.getHoldingsChange()));
					else {
						// XXX use midQuotePrice if lastPrice not defined
						value.put(spread, result.getCashChange() + (midQuotePrice * result.getHoldingsChange()));
					}
				}
			}
			ImmutableMap<Integer,Integer> valueDeltas = value.build();

			// Recalculate weights based on performances from the last timestep, 
			// using Multiplicative Weights (Abernethy&Kale 4.1)
			recalculateWeights(valueDeltas);
			log(INFO, "%s in %s: Current spread weights: %s",
					this, primaryMarket, weights.toString());

			//Submit updated order ladder, using the spread chosen by the learning algorithm
			int offset = getSpread() / 2;
			int ladderSize = stepSize * (numRungs - 1);
			withdrawAllOrders();
			submitOrderLadder(Price.of(avgPrice.intValue() - offset - ladderSize),  //minimum buy
					Price.of(avgPrice.intValue() - offset), 			 	 //maximum buy
					Price.of(avgPrice.intValue() + offset),				 //minimum sell
					Price.of(avgPrice.intValue() + offset + ladderSize)); //maximum sell
			log(INFO, "%s in %s: submitting ladder with spread %d",
					this, primaryMarket, offset * 2);
			lastPrice = Optional.of(avgPrice);
		}
		lastBid = bid;
		lastAsk = ask; 
	}

	/**
	 * Stores net change in holdings & cash
	 * 
	 * TODO get rid of this class?
	 */
	protected static class TransactionResult extends utils.Pair<Integer, Integer> {
		protected TransactionResult(int holdingsChange, int cashChange) {
			super(holdingsChange, cashChange);
		}

		public int getHoldingsChange() { return left; }
		public int getCashChange() { return right; }
	}

}
