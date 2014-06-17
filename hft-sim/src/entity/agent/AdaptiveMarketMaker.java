package entity.agent;

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

	public AdaptiveMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			int tickSize, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickInside, int initLadderMean,
			int initLadderRange, int[] spreads, boolean useMedianSpread) {

		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				numRungs, rungSize, truncateLadder, tickImprovement, tickInside,
				initLadderMean, initLadderRange);

		this.useMedianSpread = useMedianSpread;

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
				props.getAsIntArray(Keys.SPREADS, new int[]{0,10,20,30,40}),//TODO(benno): These are just placeholder values - still have to figure out reasonable defaults.  Maybe some should be negative, for more aggressive behavior?
				// XXX Erik: Seems like a fine default, although you may just
				// not want to have a defauklt. If you leave it off an error
				// will be thrown if it doesn't exist.
				props.getAsBoolean(Keys.USE_MEDIAN_SPREAD, true)
			);
	}

	/**
	 * @return a spread value: either the median or one chosen at random according to the current weights, depending on "useMedianSpread" parameter
	 */
	protected int getSpread(){
		double r = useMedianSpread ? 0.5 : rand.nextDouble();
		double p_sum = 0.0;
		for(Map.Entry<Integer,Double> e : weights.entrySet()){
			p_sum += e.getValue();
			if (p_sum >= r) { return e.getKey(); }
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
	 * @return An int array of length 2: [<net holdings change> , <net cash change>]
	 * XXX ERIK: Stylistically and for clarity, you should subclass the pair data type (as is done elsewhere) to make it clear what this is returning
	 */
	//TODO(benno):  This method could be more efficient; it would lose some clarity though, and it's relatively small anyway - linear in the size of the ladder.  Worth refactoring?
	// XXX Erik: I like it the way it is. Clear functions are much nicer than super efficient code that doesn't speed up execution
	protected int[] lastTransactionResult(int spread, int bidPrice, int askPrice){
		int delta_h = 0, delta_c = 0; //changes in holdings / cash, respectively
		int lastBidPrice = lastBid.intValue(), lastAskPrice = lastAsk.intValue();
		for(int rung = 0; rung < numRungs; rung++){
			int offset = spread + rung * stepSize;
			if(lastBidPrice - offset >= askPrice) 		//If this bid would have transacted
				{ delta_h += 1; delta_c -= (lastBid.intValue() - offset);}
			if(lastAskPrice + offset <= bidPrice) 		//If this ask would have transacted
				{ delta_h -= 1; delta_c += (lastAsk.intValue() + offset);}
		}
		return new int[]{delta_h, delta_c};
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

		//TODO(benno) There's gotta be a more sophisticated way to approximate this.
		// XXX Erik: The midpoind (what you calculated) is reasonable. You can also look at the last transaction price, which is often used.
		int approximateUnitPrice = (bid.intValue() + ask.intValue()) / 2; //Approximate the price of a single unit as the midpoint between the quoted bid and ask prices

		//For each spread, determine how it would have performed in the last round.
		Map<Integer,Integer> valueDeltas = Maps.newHashMapWithExpectedSize(weights.size()); //XXX This can be an "ImmutableMap"
		for(int spread : weights.keySet()){
			int[] transaction = lastTransactionResult(spread, bid.intValue(), ask.intValue());
			valueDeltas.put(spread, transaction[1] + (approximateUnitPrice * transaction[0]));//<change_in_cash> + <change_in_holdings>*<approximate_unit_value>
		}

		//Recalculate weights based on performances from the last timestep, using Multiplicative Weights (Abernethy&Kale 4.1)

		// From Abernethy + Kale Theorem 3.  Leaving out constant factor of 1/(2G), which would require knowledge of
		// the maximum amount the fundamental can change by in one time step.
		//TODO(benno) Is it okay to leave out that factor of 1/(2G)?  (G is defined in Lemma 4 of Abernethy's paper)
		// my hunch is yes - it's a constant factor that will disappear after normalization
		// XXX Erik: The 1/(2G) in the exponential, so it doesn't factor out.
		// You should be able to calculate a proxy for G from the parameters of
		// the simulation. e.g. (with ZIR agents) if you know their arrival rate
		// and distribution of shading and private value distribution.
		// Alternatively, you can just make G a parameter.
		// XXX Erik: Alternatively given the weighting problem, you can also
		// experiment with FTPL. This basically says you always play the ones
		// that's doing best, but with a random perturbation. I'm sure I can
		// find litarature for you if you need it.
		double eta_t = Math.min(Math.sqrt(Math.log(weights.size())/currentTime.getInTicks()), 1.0);

		for(int spread : weights.keySet()){ // XXX Erik: You can iterate over the entrySet here, and just use setValue. Slightly more efficient 
			weights.put(spread, weights.get(spread) * Math.exp(eta_t * valueDeltas.get(spread)));
		}
		normalizeWeights();
		log.log(DEBUG, "%s in %s: Current spread weights: %s", 
				this, primaryMarket, weights.toString());

		//Submit updated order ladder, using the spread chosen by the multiplicative weights algorithm
		//TODO(benno) This doesn't put any limit on long/short positions.  How to go about doing that?  Make more aggressive buy(sell) offers when you're short(long)? 
		// NOTE: Other (MAMM, BasicMM, WMAMM) market makers also don't appear to
		// put any limit... Either I'm missing something or a limit isn't
		// necessary.
		// XXX Erik: Currently none of the market makers put any limit. It
		// shouldn't really be necessary, because if we run our simulation long
		// enough, it should end up being profitable. However, if you want to
		// put a flag in for maximum position, and start dropping the ladder
		// beyond the threshold, you can do that. You'll loose theoretical
		// guaranetees, but those were already lost anyways.
		int spread = getSpread();
		withdrawAllOrders();
		createOrderLadder(bid, ask, spread);
		// XXX Erik: I know the method says bid and ask (maybe you should change
		// that), but really what it means is the lowest sell and the highest
		// bid for the ladder. Instead of doing createOrderLadder(bid, ask,
		// spread), you should do createOrderLadder(bid - spread, ask + spread).
		// XXX Erik: Also, in Jake's paper, the bid and the ask that the MM
		// supplied are independent of the markets current bid and ask. If the
		// MM calculates a spread of 0, then the ladders should start at the
		// same position. Thus, instead of the current bid and ask, you should
		// center around the midpoint or the last transaction price aka:
		// createOrderLadder(unitPrice - spread, unitPrice + spread)
		log.log(INFO, "%s in %s: submitting ladder with spread %d",
				this, primaryMarket, spread);


		// update latest bid/ask prices
		lastAsk = ask; lastBid = bid;
	}

}
