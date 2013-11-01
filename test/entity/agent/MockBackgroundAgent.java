package entity.agent;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import com.google.common.collect.ImmutableSet;

import data.FundamentalValue;
import activity.Activity;
import activity.MockActivity;
import entity.agent.BackgroundAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import event.TimeStamp;

public class MockBackgroundAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1L;

	public MockBackgroundAgent(FundamentalValue fundamental, SIP sip, Market market) {
		this(fundamental, sip, market, new PrivateValue(), 0, 1000);
	}
	
	public MockBackgroundAgent(FundamentalValue fundamental, SIP sip, Market market,
			PrivateValue pv, int bidRangeMin, int bidRangeMax) {
		super(new TimeStamp(0), fundamental, sip, market, new Random(), 0,
				pv,	1, bidRangeMin, bidRangeMax);
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		return ImmutableSet.of();
	}

	public Iterable<? extends Activity> addMockActivity(TimeStamp currentTime) {
		return Collections.singleton(new MockActivity(currentTime));
	}
	
	public Collection<Order> getOrders() {
		return this.activeOrders;
	}
	
	@Override
	public String toString() {
		return "MockBackgroundAgent " + super.toString();
	}
}
