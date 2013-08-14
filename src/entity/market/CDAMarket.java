package entity.market;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
	public Collection<? extends Activity> submitOrder(Agent agent, Price price,
			int quantity, TimeStamp curentTime, TimeStamp duration) {
		Collection<Activity> activities = new ArrayList<Activity>(
				super.submitOrder(agent, price, quantity, curentTime, duration));
		activities.add(new Clear(this, TimeStamp.IMMEDIATE));
		return activities;
	}

	@Override
	public Collection<? extends Activity> withdrawOrder(Order order,
			TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>(2);
		acts.addAll(super.withdrawOrder(order, currentTime));
		acts.addAll(updateQuote(Collections.<Transaction>emptyList(), currentTime));
		return acts;
	}

}
