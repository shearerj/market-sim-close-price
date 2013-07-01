package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import market.PrivateValue;
import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import data.ObjectProperties;
import data.Observations;
import data.SystemData;
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

	protected int bidRange;				// range for limit order
	protected int sleepTime, sleepVar;
	protected double c_R, c_A, beta, betaVar, gamma;
	
	public ZIPAgent(int agentID, TimeStamp arrivalTime, MarketModel model,
			Market market, RandPlus rand, ObjectProperties props) {
		super(agentID, arrivalTime, model, market, rand);
		this.bidRange = props.getAsInt(BIDRANGE_KEY, 2000);
		this.sleepTime = props.getAsInt(SLEEPTIME_KEY, 50);
		this.sleepVar = props.getAsInt(SLEEPVAR_KEY, 100);
		this.c_R = props.getAsDouble("c_R", .05);
		this.c_A = props.getAsDouble("c_A", .05);
		this.beta = props.getAsDouble("beta", .03);
		this.betaVar = props.getAsDouble("betaVar", .005);
		this.gamma = props.getAsDouble("gamma", .5);
	}
	
	public ZIPAgent(int agentID, int modelID, SystemData d, ObjectProperties p) {
		super(agentID, modelID, d, p);
		
		bidRange = params.getAsInt(ZIAgent.BIDRANGE_KEY);
		int alpha1 = (int) Math.round(rand.nextGaussian(0, this.data.pvVar));
		int alpha2 = (int) Math.round(rand.nextGaussian(0, this.data.pvVar));
		alpha = new PrivateValue(alpha1, alpha2);
	}

	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String,Object> obs = new HashMap<String,Object>();
		obs.put(Observations.ROLE_KEY, getRole());
		obs.put(Observations.PAYOFF_KEY, getRealizedProfit());
		obs.put(Observations.STRATEGY_KEY, getFullStrategy());
		return obs;
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		int q = 1;
		// 0.50% chance of being either long or short
		if (rand.nextDouble() < 0.5) q = -q;
		@SuppressWarnings("unused")
		int val = Math.max(0, data.getFundamentalAt(ts).sum(getPrivateValueAt(q)).getPrice());

		// Insert events for the agent to sleep, then wake up again at timestamp tsNew
		int sleepTime = params.getAsInt(SLEEPTIME_KEY);
		double sleepVar = params.getAsDouble(SLEEPVAR_KEY);
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		actMap.add(new AgentStrategy(this, tsNew));
		return actMap;
	}
}
