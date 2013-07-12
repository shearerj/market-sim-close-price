package entity;

import java.util.ArrayList;
import java.util.Collection;

import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import data.EntityProperties;
import data.Keys;
import event.TimeStamp;

/**
 * Use kappa to set 'c' for the ZIP agent
 * 
 * Use \mu from Cliff's paper
 * 
 * Ensure that transaction price != \lambda
 * 
 * 
 * 
 * @author ewah, sgchako, kunshao, marzuq, gshiva
 * 
 */
public class ZIPAgent extends BackgroundAgent {

	protected int bidRange; // range for limit order
	protected int sleepTime, sleepVar;
	protected double c_R, c_A, beta, betaVar, gamma;

	public ZIPAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, SIP sip, EntityProperties props) {
		// TODO replace null with proper private value initialization
		super(agentID, arrivalTime, model, market, null, rand, sip,
				props.getAsInt("tickSize", 1000));
		this.bidRange = props.getAsInt(Keys.BID_RANGE, 2000);
		this.sleepTime = props.getAsInt(Keys.SLEEP_TIME, 50);
		this.sleepVar = props.getAsInt(Keys.SLEEP_VAR, 100);
		this.c_R = props.getAsDouble("c_R", .05);
		this.c_A = props.getAsDouble("c_A", .05);
		this.beta = props.getAsDouble("beta", .03);
		this.betaVar = props.getAsDouble("betaVar", .005);
		this.gamma = props.getAsDouble("gamma", .5);
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp currentTime) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		// 0.50% chance of being either long or short
		int quantity = rand.nextBoolean() ? 1 : -1;
		@SuppressWarnings("unused")
		int val = Math.max(
				0,
				model.getFundamentalAt(currentTime).plus(
						privateValue.getValueFromQuantity(positionBalance,
								quantity)).getPrice());

		// Insert events for the agent to sleep, then wake up again at timestamp
		// tsNew
		int sleepTime = params.getAsInt(Keys.SLEEP_TIME);
		double sleepVar = params.getAsDouble(Keys.SLEEP_VAR);
		TimeStamp tsNew = currentTime.plus(new TimeStamp(
				(long) rand.nextGaussian(sleepTime, sleepVar)));
		actMap.add(new AgentStrategy(this, tsNew));
		return actMap;
	}
}
