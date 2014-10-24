package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Random;

import systemmanager.Keys.NumHistorical;
import systemmanager.Simulation;

import com.google.common.base.Optional;
import com.google.common.collect.EvictingQueue;
import com.google.common.math.DoubleMath;

import data.Props;
import entity.market.Market;
import entity.market.Price;

/**
 * MAMARKETMAKER
 * 
 * Moving Average Market Maker
 * 
 * NOTE: Because the prices are stored in an EvictingQueue, which does not
 * accept null elements, the number of elements in the bid/ask queues may not be
 * equivalent.
 * 
 * @author zzy, ewah
 */
/*
 * TODO I think class would make more sense if it were abstract, and took
 * average function as an input parameter. Then instead of WMA there's
 * exponential weighted and linear weighted that pass in different functions.
 */
public class MAMarketMaker extends BasicMarketMaker {

	private static final long serialVersionUID = -4766539518925397355L;

	protected final EvictingQueue<Price> bidQueue, askQueue;

	protected MAMarketMaker(Simulation sim, Market market, Random rand, Props props) {
		super(sim, market, rand, props);
		int numHistorical = props.get(NumHistorical.class);
		checkArgument(numHistorical > 0, "Number of historical prices must be positive!");
		bidQueue = EvictingQueue.create(numHistorical);
		askQueue = EvictingQueue.create(numHistorical);
	}

	public static MAMarketMaker create(Simulation sim, Market market, Random rand, Props props) {
		return new MAMarketMaker(sim, market, rand, props);
	}

	@Override
	protected void submitCalculatedSpread(Optional<Price> bid, Optional<Price> ask) {
		if (bid.isPresent())
			bidQueue.add(bid.get());
		if (ask.isPresent())
			askQueue.add(ask.get());
		
		createOrderLadder(
				(bidQueue.isEmpty() ? Optional.<Price> absent() : Optional.of(Price.of(average(bidQueue)))),
				(askQueue.isEmpty() ? Optional.<Price> absent() : Optional.of(Price.of(average(askQueue)))));
	}
	
	protected double average(Iterable<? extends Number> numbers) {
		return DoubleMath.mean(numbers);
	}

}

