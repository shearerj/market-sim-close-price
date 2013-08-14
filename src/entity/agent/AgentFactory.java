package entity.agent;

import java.util.Collection;

import generator.Generator;
import generator.PoissonArrivalGenerator;
import generator.RoundRobinGenerator;

import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class AgentFactory {

	protected final RandPlus rand;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Generator<TimeStamp> arrivalProcess;
	protected final Generator<Market> marketAssignment;

	public AgentFactory(FundamentalValue fundamental, SIP sip,
			Generator<TimeStamp> arrivalProcess, Collection<Market> markets,
			Generator<Market> marketProcess, RandPlus rand) {
		this.rand = rand;
		this.fundamental = fundamental;
		this.sip = sip;
		this.markets = markets;
		this.arrivalProcess = arrivalProcess;
		this.marketAssignment = marketProcess;
	}

	/**
	 * SMAgent factory with Poisson arrivals and round robin market selection.
	 */
	public AgentFactory(FundamentalValue fundamental, SIP sip,
			Collection<Market> markets, double arrivalRate, RandPlus rand) {
		this(fundamental, sip, new PoissonArrivalGenerator(TimeStamp.IMMEDIATE,
				arrivalRate, new RandPlus(rand.nextLong())), markets,
				new RoundRobinGenerator<Market>(markets), rand);
	}

	// XXX Not all agents advance all of the parameters like the market or the arrival process. One
	// just has to be sure that this happens appropriately if one factory is creating several
	// different classes of agents.
	public Agent createAgent(AgentProperties props) {
		switch (props.getAgentType()) {
		case AA:
			return new AAAgent(arrivalProcess.next(), fundamental, sip,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZIP:
			return new ZIPAgent(arrivalProcess.next(), fundamental, sip,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZIR:
			return new ZIRAgent(arrivalProcess.next(), fundamental, sip,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZI:
			return new ZIAgent(arrivalProcess.next(), fundamental, sip,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case BASICMM:
			return new BasicMarketMaker(fundamental, sip,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case LA:
			return new LAAgent(markets, fundamental, sip, new RandPlus(
					rand.nextLong()), props);
		default:
			throw new IllegalArgumentException("Can't create AgentType: "
					+ props.getAgentType());
		}
	}

}
