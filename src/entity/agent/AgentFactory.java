package entity.agent;

import generator.Generator;
import generator.PoissonArrivalGenerator;
import generator.RoundRobinGenerator;

import model.MarketModel;

import utils.RandPlus;
import data.AgentProperties;
import entity.market.Market;
import event.TimeStamp;

public class AgentFactory {

	protected final RandPlus rand;
	protected final MarketModel model;
	protected final Generator<TimeStamp> arrivalProcess;
	protected final Generator<Market> marketAssignment;

	public AgentFactory(MarketModel model, Generator<TimeStamp> arrivalProcess,
			Generator<Market> marketProcess, RandPlus rand) {
		this.rand = rand;
		this.model = model;
		this.arrivalProcess = arrivalProcess;
		this.marketAssignment = marketProcess;
	}

	/**
	 * SMAgent factory with Poisson arrivals and round robin market selection.
	 */
	public AgentFactory(MarketModel model, double arrivalRate, RandPlus rand) {
		this(model, new PoissonArrivalGenerator(TimeStamp.IMMEDIATE,
				arrivalRate, new RandPlus(rand.nextLong())),
				new RoundRobinGenerator<Market>(model.getMarkets()), rand);
	}

	// XXX Not all agents advance all of the parameters like the market or the arrival process. One
	// just has to be sure that this happens appropriately if one factory is creating several
	// different classes of agents.
	public Agent createAgent(AgentProperties props) {
		switch (props.getAgentType()) {
		case AA:
			return new AAAgent(arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZIP:
			return new ZIPAgent(arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZIR:
			return new ZIRAgent(arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case ZI:
			return new ZIAgent(arrivalProcess.next(), model,
					marketAssignment.next(), new RandPlus(rand.nextLong()),
					props);
		case BASICMM:
			return new BasicMarketMaker(model, marketAssignment.next(),
					new RandPlus(rand.nextLong()), props);
		case LA:
			return new LAAgent(model, new RandPlus(rand.nextLong()), props);
		default:
			throw new IllegalArgumentException("Can't create AgentType: "
					+ props.getAgentType());
		}
	}

}
