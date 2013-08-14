package entity.agent;

import java.util.ArrayList;
import java.util.Collection;

import systemmanager.Keys;
import utils.RandPlus;
import activity.Activity;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
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
public class ZIPAgent extends ReentryAgent {

	private static final long serialVersionUID = 8138883791556301413L;
	
	protected final int bidRange; // range for limit order
	protected final double c_R, c_A, beta, betaVar, gamma;

	public ZIPAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, Market market,
			RandPlus rand, double pvVar, int tickSize, int bidRange,
			double reentryRate, double c_R, double c_A, double beta,
			double betaVar, double gamma) {
		super(arrivalTime, fundamental, sip, market, new PrivateValue(1,
				pvVar, rand),
				rand, reentryRate, tickSize);
		this.bidRange = bidRange;
		this.c_R = c_R;
		this.c_A = c_A;
		this.beta = beta;
		this.betaVar = betaVar;
		this.gamma = gamma;
	}
	
	public ZIPAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, Market market,
			RandPlus rand, EntityProperties props) {
		this(arrivalTime, fundamental, sip, market, rand, props.getAsDouble(
				Keys.PRIVATE_VALUE_VAR, 100000000), props.getAsInt(
				Keys.TICK_SIZE, 1), props.getAsInt(Keys.BID_RANGE, 5000),
				props.getAsDouble(Keys.REENTRY_RATE, 0.005), props.getAsDouble(
						Keys.CR, .05), props.getAsDouble(Keys.CA, .05),
				props.getAsDouble(Keys.BETA, .03), props.getAsDouble(
						Keys.BETA_VAR, .005), props.getAsDouble(Keys.BETA, .5));
	}
	
	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		Collection<Activity> acts = new ArrayList<Activity>(super.agentStrategy(currentTime));
		
		// 0.50% chance of being either long or short
		int quantity = rand.nextBoolean() ? 1 : -1;
		@SuppressWarnings("unused")
		int val = Math.max(
				0,
				fundamental.getValueAt(currentTime).plus(
						privateValue.getValueFromQuantity(positionBalance,
								quantity)).getInTicks());

		// Insert events for the agent to sleep, then wake up again at timestamp
		// tsNew
		return acts;
	}
}
