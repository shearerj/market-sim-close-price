package entity.market;

import java.util.Random;

import logger.Log;
import systemmanager.Keys.MarketTickSize;
import systemmanager.Keys.TickSize;
import data.Props;
import data.Stats;
import entity.agent.Agent.AgentView;
import entity.agent.OrderRecord;
import entity.market.clearingrule.EarliestPriceClear;
import entity.sip.MarketInfo;
import event.Timeline;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	private static final long serialVersionUID = -6780130359417129449L;

	protected CDAMarket(int id, Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip, Props props) {
		super(id, stats, timeline, log, rand, sip, new EarliestPriceClear(props.get(MarketTickSize.class, TickSize.class)), props);
	}

	public static CDAMarket create(int id, Stats stats, Timeline timeline, Log log, Random rand, MarketInfo sip, Props props) {
		return new CDAMarket(id, stats, timeline, log, rand, sip, props);
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
