package entity.market;

import com.google.common.collect.ImmutableList;

import systemmanager.Keys;

import data.EntityProperties;

import activity.Activity;
import activity.Clear;
import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.clearingrule.EarliestPriceClear;
import event.TimeStamp;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	private static final long serialVersionUID = -6780130359417129449L;

	public CDAMarket(SIP sip, TimeStamp latency) {
		super(sip, new EarliestPriceClear(), latency);
	}
	
	public CDAMarket(SIP sip, EntityProperties props) {
		this(sip, new TimeStamp(props.getAsInt(Keys.MARKET_LATENCY, -1)));
	}

	@Override
	public Iterable<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp curentTime, TimeStamp duration) {
		return ImmutableList.<Activity> builder().addAll(
				super.submitOrder(agent, price, quantity, curentTime, duration)).add(
				new Clear(this, TimeStamp.IMMEDIATE)).build();
	}

	@Override
	public Iterable<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		return ImmutableList.<Activity> builder().addAll(
				super.withdrawOrder(order, currentTime)).addAll(
				updateQuote(ImmutableList.<Transaction> of(), currentTime)).build();
	}

}
