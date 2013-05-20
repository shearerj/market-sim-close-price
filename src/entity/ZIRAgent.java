package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;

/**
 * ZIRAGENT
 *
 * A zero-intelligence agent with re-submission (ZIR).
 *
 * The ZIR agent is primarily associated with a single market. It wakes up
 * periodically to submit a new bid (if its previous bid has transacted) or it
 * does nothing.
 * 
 * This agent bases its private value on a stochastic process, the parameters
 * of which are specified at the beginning of the simulation by the spec file.
 * The agent's private valuation is determined by value of the random process at
 * the time it enters, with some randomization added by using an individual 
 * variance parameter. The private value is used to calculate the agent's surplus 
 * (and thus the market's allocative efficiency).
 *
 * This agent submits a single limit order at a time. It will modify its private
 * value if its bid has transacted by the time it wakes up.
 * 
 * NOTE: Each limit order price is uniformly distributed over a range that is twice
 * the size of bidRange in either a positive or negative direction from the agent's
 * private value.
 *
 *
 * TODO - need to know how many arrival times to store? or just compute dynamically?
 *
 *
 * @author ewah
 */
public class ZIRAgent extends BackgroundAgent {

	private int bidRange;					// range for limit order
	private ArrivalTime reentry;			// re-entry times
	private int lastPositionBalance;		// last position balance
	private int maxAbsPosition;				// max quantity for position
	
	// have to keep track of submission times for each order
	// for tracking discounted surplus
	private ArrayList<TimeStamp> submissionTimes;
	

	/**
	 * Overloaded constructor.
	 */
	public ZIRAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		
		rand = new Random(Long.parseLong(params.get(Agent.RANDSEED_KEY)));
		arrivalTime = new TimeStamp(Long.parseLong(params.get(Agent.ARRIVAL_KEY)));
		bidRange = Integer.parseInt(params.get(ZIRAgent.BIDRANGE_KEY));
		reentry = new ArrivalTime(arrivalTime, this.data.reentryRate, rand);
		maxAbsPosition = Integer.parseInt(params.get(ZIRAgent.MAXQUANTITY_KEY));
		lastPositionBalance = positionBalance;
		
		submissionTimes = new ArrayList<TimeStamp>();
		
		ArrayList<Integer> alphas = new ArrayList<Integer>();
		for (int i = -maxAbsPosition; i <= maxAbsPosition; i++) {
			if (i != 0)	alphas.add((int) Math.round(getNormalRV(0, this.data.pvVar)));
		}
		alpha = new PrivateValue(alphas);
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
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();

		String s = "";
		if (!ts.equals(arrivalTime)) {
			s += ts + " | " + this + " wake up. ";
			if (positionBalance == lastPositionBalance) {
				s += "last order has not transacted, go back to sleep";
			}
		}
		if (positionBalance != lastPositionBalance || ts.equals(arrivalTime)) {
			// If either first arrival or if last order has already transacted then should 
			// submit a new order. Otherwise, do nothing.
			// TODO ZIR: should it be able to cancel orders?
			
			int p = 0;
			int q = 1;
			// 0.50% chance of being either long or short
			if (rand.nextDouble() < 0.5) q = -q;
			
			int val = 0;
			int newPosition = q + positionBalance;
			// check that will not exceed max absolute position
			if (newPosition <= maxAbsPosition && newPosition >= -maxAbsPosition) {
				val = Math.max(0, data.getFundamentalAt(ts).sum(getPrivateValueAt(q)).getPrice());
				s += "position=" + positionBalance + ", for q=" + q + ", value=" + 
						data.getFundamentalAt(ts) + " + " + getPrivateValueAt(q) + "=" + val;
				
				if (q > 0) {
					p = (int) Math.max(0, ((val - 2*bidRange) + rand.nextDouble()*2*bidRange));
				} else {
					p = (int) Math.max(0, (val + rand.nextDouble()*2*bidRange));
				}
				actMap.appendActivityHashMap(submitNMSBid(p, q, ts));	// bid does not expire
				submissionTimes.add(ts);
				
				lastPositionBalance = positionBalance;	// update position balance
				
//				System.out.println(ts + " | " + this + " positionBalance=" + positionBalance + 
//						", " + data.getFundamentalAt(ts) + "+" + getPrivateValueAt(q) + "=" + val + ", q=" + q);

			} else {
//				log.log(Log.INFO, ts + " | " + this + " new order would exceed " +
//						"max position " + maxAbsPosition);
				s += "new order would exceed max position " + maxAbsPosition 
						+ "; no submission";
			}
			
			// if exceed max position, then don't submit a new bid
			// TODO - stay the same for now (position balance)
		}
		log.log(Log.INFO, s);
		
		TimeStamp tsNew = reentry.next();	// compute next re-entry time
		actMap.insertActivity(Consts.SM_AGENT_PRIORITY, new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(Consts.SM_AGENT_PRIORITY, new AgentStrategy(this, market, tsNew));
		return actMap;
	}

	/**
	 * @return ArrivalTime object holding re-entries into market
	 */
	public ArrivalTime getReentryTimes() {
		return reentry;
	}
}
