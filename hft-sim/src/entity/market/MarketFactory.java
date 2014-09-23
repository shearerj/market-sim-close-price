package entity.market;

import java.util.Random;

import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.Simulation;
import data.Props;

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
			if (props.getAsInt(Keys.CLEAR_FREQ) <= 0)
				return CDAMarket.create(sim, new Random(rand.nextLong()), props);
			else
				return CallMarket.create(sim, new Random(rand.nextLong()), props);
		default:
			throw new IllegalArgumentException("Can't create MarketType: " + type);
		}
	}

}
