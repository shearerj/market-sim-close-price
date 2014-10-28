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

	private final Simulation sim;
	private final Random rand;
	private final Collection<Market> markets;
	private final Iterator<Market> marketAssignment;

	private AgentFactory(Simulation sim, Collection<Market> markets, Iterator<Market> marketProcess, Random rand) {
		this.sim = sim;
		this.rand = rand;
		this.markets = markets;
		this.marketAssignment = marketProcess;
	}
	
	/** Factory with round robin scheduling */
	public static AgentFactory create(Simulation sim, Collection<Market> markets, Random rand) {
		return new AgentFactory(sim, markets, Iterators.cycle(markets), rand);
	}

	// All Agents should advance rand, marketAssignment, and arrivlProcess
	@SuppressWarnings("deprecation")
	public Agent createAgent(AgentType type, Props props) {
		switch (type) {
		case AA:
			return AAAgent.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case ZIP:
			return ZIPAgent.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case ZIRP:
			return ZIRPAgent.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case ZIR:
			return ZIRAgent.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case ZI:
			return ZIAgent.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case MARKETDATA:
			return MarketDataAgent.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case BASICMM:
			return BasicMarketMaker.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case MAMM:
			return MAMarketMaker.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case WMAMM:
			return WMAMarketMaker.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case ADAPTIVEMM:
			return AdaptiveMarketMaker.create(sim, marketAssignment.next(), new Random(rand.nextLong()), props);
		case LA:
			marketAssignment.next();
			return LAAgent.create(sim, markets, new Random(rand.nextLong()), props);
		case NOOP:
			marketAssignment.next();
			return NoOpAgent.create(sim, new Random(rand.nextLong()), props);

		default:
			throw new IllegalArgumentException("Can't create AgentType: " + type);
		}
	}

	// FIXME Move this to background agent?
	public static Iterator<TimeStamp> exponentials(double rate, Random rand) {
		return Iterators.transform(Iterators2.exponentials(rate, rand), new Function<Double, TimeStamp>(){
			@Override public TimeStamp apply(Double dub) { return TimeStamp.of((long) (double) dub); }
		});
	}
	
}
