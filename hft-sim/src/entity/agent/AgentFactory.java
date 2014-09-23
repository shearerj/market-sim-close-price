package entity.agent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import systemmanager.Consts.AgentType;
import systemmanager.Simulation;
import utils.Iterators2;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import data.Props;
import entity.market.Market;
import event.TimeStamp;

public class AgentFactory {

	protected final Simulation sim;
	protected final Random rand;
	protected final Collection<Market> markets;
	protected final Iterator<TimeStamp> arrivalProcess;
	protected final Iterator<Market> marketAssignment;

	protected AgentFactory(Simulation sim, Iterator<TimeStamp> arrivalProcess,
			Collection<Market> markets, Iterator<Market> marketProcess,
			Random rand) {
		
		this.sim = sim;
		this.rand = rand;
		this.markets = markets;
		this.arrivalProcess = arrivalProcess;
		this.marketAssignment = marketProcess;
	}

	/**
	 * SMAgent factory with Poisson arrivals and round robin market selection.
	 */
	public static AgentFactory create(Simulation sim, Collection<Market> markets, double arrivalRate, Random rand) {
		return new AgentFactory(sim, exponentials(arrivalRate, new Random(rand.nextLong())),
				markets, Iterators.cycle(markets), rand);
	}

	// All Agents should advance rand, marketAssignment, and arrivlProcess
	@SuppressWarnings("deprecation")
	public Agent createAgent(AgentType type, Props props) {
		switch (type) {
		case AA:
			return AAAgent.create(sim, arrivalProcess.next(), marketAssignment.next(), new Random(rand.nextLong()), props);
		case ZIP:
			return ZIPAgent.create(sim, arrivalProcess.next(), marketAssignment.next(), new Random(rand.nextLong()), props);
		case ZIRP:
			return ZIRPAgent.create(sim, arrivalProcess.next(), marketAssignment.next(), new Random(rand.nextLong()), props);
		case ZIR:
			return ZIRAgent.create(sim, arrivalProcess.next(), marketAssignment.next(), new Random(rand.nextLong()), props);
		case ZI:
			return ZIAgent.create(sim, arrivalProcess.next(), marketAssignment.next(), new Random(rand.nextLong()), props);
		case MARKETDATA:
			return MarketDataAgent.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case BASICMM:
			arrivalProcess.next();
			return BasicMarketMaker.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case MAMM:
			arrivalProcess.next();
			return MAMarketMaker.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case WMAMM:
			arrivalProcess.next();
			return WMAMarketMaker.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case ADAPTIVEMM:
			arrivalProcess.next();
			return AdaptiveMarketMaker.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case LA:
			arrivalProcess.next();
			marketAssignment.next();
			return LAAgent.create(sim, markets, new Random(rand.nextLong()), props);
		case NOOP:
			arrivalProcess.next();
			marketAssignment.next();
			return NoOpAgent.create(sim, new Random(rand.nextLong()), props);

		default:
			throw new IllegalArgumentException("Can't create AgentType: " + type);
		}
	}

	public static Iterator<TimeStamp> exponentials(double rate, Random rand) {
		return Iterators.transform(Iterators2.exponentials(rate, rand), new Function<Double, TimeStamp>(){
			@Override public TimeStamp apply(Double dub) { return TimeStamp.of((long) (double) dub); }
		});
	}
	
}
