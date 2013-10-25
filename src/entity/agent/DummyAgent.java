package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

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

public class DummyAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1L;

	public DummyAgent(FundamentalValue fundamental, SIP sip, Market market) {
		super(new TimeStamp(0), fundamental, sip, market, new Random(), 0,
				new PrivateValue(),	1, 0, 1000);
		
		/*String message = "PHAgent created!";
		log(INFO, message);*/
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		
		/*String message = "PHAgent has no strategy!";
		log(INFO, message);*/
		return ImmutableSet.of(); // Returns an empty set. 
		
	}

	public Collection<Order> getOrders() {
		//String message = "PHAgent has no orders!";
		//log(INFO, message);
		return this.activeOrders;
	}
	
	@Override
	public String toString() {
		
		return "DummyAgent " + super.toString();
	}
}
