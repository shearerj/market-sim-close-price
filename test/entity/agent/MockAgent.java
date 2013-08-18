package entity.agent;

import java.util.Collection;

import com.google.common.collect.ImmutableSet;

import data.FundamentalValue;

import utils.Rands;
import activity.Activity;
import entity.agent.BackgroundAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MockAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1L;

	public MockAgent(FundamentalValue fundamental, SIP sip, Market market) {
		super(new TimeStamp(0), fundamental, sip, market, new PrivateValue(),
				new Rands(), 1000);
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		return ImmutableSet.of();
	}

}
