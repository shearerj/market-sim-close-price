package entity;

import generators.Generator;
import generators.PoissonArrivalGenerator;
import generators.RoundRobinGenerator;
import model.MarketModel;
import systemmanager.Consts;
import utils.RandPlus;

public class PRRSMAgentFactory extends SMAgentFactory {

	public PRRSMAgentFactory(MarketModel model, Generator<Integer> ids,
			long arrivalRate, RandPlus rand) {
		super(model, ids, new PoissonArrivalGenerator(Consts.START_TIME,
				arrivalRate, new RandPlus(rand.nextLong())),
				new RoundRobinGenerator<Market>(model.getMarkets()), rand);
	}

}
