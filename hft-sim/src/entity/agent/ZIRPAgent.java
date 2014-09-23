package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Simulation;
import data.Props;
import entity.market.Market;
import event.TimeStamp;
import fourheap.Order.OrderType;

public final class ZIRPAgent extends BackgroundAgent {
	
	private static final long serialVersionUID = -8805640643365079141L;
	
	protected ZIRPAgent(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, Props props) {
		super(sim, arrivalTime, market, rand, props);
	}
	
	public static ZIRPAgent create(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, Props props) {
		return new ZIRPAgent(sim, arrivalTime, market, rand, props);
	}

	@Override
	public void agentStrategy() {
		super.agentStrategy();
		
		// 50% chance of being either long or short
		OrderType orderType = rand.nextBoolean() ? BUY : SELL;
		log(INFO, "%s Submit %s order", this, orderType);
		executeZIRPStrategy(orderType, 1);
	}
}
