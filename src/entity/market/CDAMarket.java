package entity.market;

import java.util.Random;

import com.google.common.collect.ImmutableList;

import systemmanager.Keys;
import data.EntityProperties;
import activity.Activity;
import activity.Clear;
import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.clearingrule.EarliestPriceClear;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	private static final long serialVersionUID = -6780130359417129449L;

	public CDAMarket(SIP sip, Random rand, TimeStamp latency, int tickSize) {
		this(sip, rand, latency, latency, tickSize);
	}
	
	public CDAMarket(SIP sip, Random rand, TimeStamp quoteLatency, 
			TimeStamp transactionLatency, int tickSize) {
		super(sip, quoteLatency, transactionLatency, new EarliestPriceClear(tickSize), rand);
	}
	
	public CDAMarket(SIP sip, Random rand, EntityProperties props) {
		this(sip, rand,
				new TimeStamp(props.getAsInt(Keys.QUOTE_LATENCY, props.getAsInt(Keys.MARKET_LATENCY, -1))),
				new TimeStamp(props.getAsInt(Keys.TRANSACTION_LATENCY, props.getAsInt(Keys.MARKET_LATENCY, -1))),
				props.getAsInt(Keys.TICK_SIZE, 1));
	}

	@Override
	public Iterable<? extends Activity> submitOrder(Agent agent, OrderType type, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		return ImmutableList.<Activity> builder().addAll(
				super.submitOrder(agent, type, price, quantity, currentTime, duration)).add(
				new Clear(this, TimeStamp.IMMEDIATE)).build();
	}

	@Override
	public Iterable<? extends Activity> withdrawOrder(Order order, int quantity,
			TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.withdrawOrder(order, quantity, currentTime)).addAll(
				updateQuote(currentTime)).build();
	}

}
