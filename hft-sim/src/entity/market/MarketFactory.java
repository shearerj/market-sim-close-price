package entity.market;

import java.util.Random;

import systemmanager.Consts.MarketType;
import systemmanager.Keys.ClearFrequency;
import systemmanager.Simulation;
import data.Props;
import event.TimeStamp;

public class MarketFactory {
	
	private final Simulation sim;
	private final Random rand;

	protected MarketFactory(Simulation sim, Random rand) {
		this.sim = sim;
		this.rand = rand;
	}
	
	public static MarketFactory create(Simulation sim, Random rand) {
		return new MarketFactory(sim, rand);
	}

	public Market createMarket(MarketType type, Props props) {
		switch (type) {
		case CDA:
			return CDAMarket.create(sim, new Random(rand.nextLong()), props);
		case CALL:
			if (props.get(ClearFrequency.class).after(TimeStamp.ZERO))
				return CallMarket.create(sim, new Random(rand.nextLong()), props);
			else
				return CDAMarket.create(sim, new Random(rand.nextLong()), props);
		default:
			throw new IllegalArgumentException("Can't create MarketType: " + type);
		}
	}

}
