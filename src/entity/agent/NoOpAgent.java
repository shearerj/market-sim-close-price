package entity.agent;

import java.util.Random;

import systemmanager.Keys;

import com.google.common.collect.ImmutableList;

import data.EntityProperties;
import data.FundamentalValue;

import activity.Activity;
import entity.infoproc.SIP;
import event.TimeStamp;

public class NoOpAgent extends Agent {
	
	private static final long serialVersionUID = -7232513254416667984L;

	public NoOpAgent(FundamentalValue fundamental, SIP sip, Random rand,
			int tickSize) {
		super(TimeStamp.ZERO, fundamental, sip, rand, tickSize);
	}

	public NoOpAgent(FundamentalValue fundamental, SIP sip, Random rand,
			EntityProperties props) {
		this(fundamental, sip, rand, props.getAsInt(Keys.TICK_SIZE, 1));
	}

	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		return ImmutableList.of();
	}

	@Override
	public String toString() {
		return "NoOpAgent " + super.toString();
	}
	
}
