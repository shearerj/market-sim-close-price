package entity.market;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Simulation;
import data.Props;
import entity.agent.Agent.AgentView;
import entity.agent.OrderRecord;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	private static final long serialVersionUID = -6780130359417129449L;

	protected CDAMarket(Simulation sim, Random rand, Props props) {
		super(sim, new EarliestPriceClear(props.getAsInt(Keys.MARKET_TICK_SIZE, Keys.TICK_SIZE)), rand, props);
	}

	public static CDAMarket create(Simulation sim, Random rand, Props props) {
		return new CDAMarket(sim, rand, props);
	}

	@Override
	protected void submitOrder(MarketView view, AgentView agent, OrderRecord order) {
		super.submitOrder(view, agent, order);
		clear();
	}

	@Override
	protected void withdrawOrder(OrderRecord order, int quantity) {
		super.withdrawOrder(order, quantity);
		updateQuote();
	}

}
