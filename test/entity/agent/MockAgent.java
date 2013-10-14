package entity.agent;

import java.util.Collection;
import java.util.Random;

import com.google.common.collect.ImmutableSet;

import data.FundamentalValue;
import activity.Activity;
import entity.agent.BackgroundAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import event.TimeStamp;

public class MockAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1L;

	public MockAgent(FundamentalValue fundamental, SIP sip, Market market) {
		super(new TimeStamp(0), fundamental, sip, market, new Random(), 0,
				new PrivateValue(),	1, 0, 1000);
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
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
