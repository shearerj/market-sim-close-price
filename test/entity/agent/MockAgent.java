package entity.agent;

import java.util.Collection;
import java.util.Random;

import com.google.common.collect.ImmutableSet;

import activity.Activity;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import event.TimeStamp;

public class MockAgent extends Agent {

	private static final long serialVersionUID = 1L;

	public MockAgent(FundamentalValue fundamental, SIP sip, Market market) {
		super(TimeStamp.ZERO, fundamental, sip, new Random(), 1);
	}

	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		return ImmutableSet.of();
	}
	
	public Collection<Order> getOrders() {
		return this.activeOrders;
	}
	
	@Override
	public String toString() {
		return "MockAgent " + super.toString();
	}
}
