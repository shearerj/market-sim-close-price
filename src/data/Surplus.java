package data;

import java.util.Collection;
import java.util.HashMap;

/**
 * Wrapper class for storing surplus for a given rho (discount factor).
 * Internal HashMap surplus is hashed by agentID, values are surplus
 * for each agent.
 * 
 * The value of rho here is only for reference later.
 * 
 * @author ewah
 *
 */
public class Surplus {

	private double rho;
	private HashMap<Integer,Double> surplus;
	
	public Surplus(double rho) {
		this.rho = rho;
		surplus = new HashMap<Integer,Double>();
	}
	
	public double getRho() {
		return rho;
	}
	
	public HashMap<Integer,Double> getSurplus() {
		return surplus;
	}
	
	public Collection<Integer> agents() {
		return surplus.keySet();
	}
	
	public Collection<Double> values() {
		return surplus.values();
	}
	
	public int size() {
		return surplus.size();
	}
	
	public double get(int agentID) {
		return surplus.get(agentID);
	}
	
	public void add(int agentID, double s) {
		surplus.put(agentID, s);
	}
	
	/**
	 * Add value s to surplus stored for this agent.
	 * @param agentID
	 * @param s
	 */
	public void addCumulative(int agentID, double s) {
		if (surplus.containsKey(agentID)) {
			double val = 0;
			if (surplus.get(agentID) != null) {
				val += surplus.get(agentID);
			}
			surplus.put(agentID, s + val);
		} else {
			surplus.put(agentID, s);
		}
	}

	
//	/**
//	 * Iterates through all transactions and sums up surplus for all agents of a 
//	 * specified type. For agents that do not have a private valuation, use their
//	 * realized profit instead.
//	 * 
//	 * Note that private values are a deviation from the fundamental (v).
//	 * 
//	 * CS = (PV + v) - p, PS = p - (PV + v)
//	 * 
//	 * @param modelID 		model id to check
//	 * @param agentType
//	 * @return hash map of agent surplus, hashed by agent ID
//	 */
//	public HashMap<Integer,Integer> getSurplusForType(int modelID, String agentType) {
//
//		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
//		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
//		
//		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
//			PQTransaction t = trans.getValue();
//			
//			if (ids.contains(t.marketID)) {
//				Agent buyer = agents.get(t.buyerID);
//				Agent seller = agents.get(t.sellerID);
//				
//				if (agentType.equals(buyer.getType())) {
//					int surplus = 0;
//					if (allSurplus.containsKey(buyer.getID())) {
//						surplus = allSurplus.get(buyer.getID());
//					}
//					if (buyer.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						int val = (buyer.getPrivateValueAt(t.quantity).sum(rt)).diff(t.price).getPrice();
//						allSurplus.put(buyer.getID(), surplus + val);
//					} else {
//						allSurplus.put(buyer.getID(), buyer.getRealizedProfit());
//					}
//				}
//				if (agentType.equals(seller.getType())) {
//					int surplus = 0;
//					if (allSurplus.containsKey(seller.getID())) {
//						surplus = allSurplus.get(seller.getID());
//					}
//					if (seller.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						int val = t.price.diff(seller.getPrivateValueAt(-t.quantity).sum(rt)).getPrice();
//						allSurplus.put(seller.getID(), surplus + val);
//					} else {
//						allSurplus.put(seller.getID(), seller.getRealizedProfit());
//					}
//				}
//			}
//		}
//		return allSurplus;
//	}
	
	
//	/**
//	 * Get total surplus for a specific agent within a model.
//	 * 
//	 * @param modelID
//	 * @param agentID
//	 * @return
//	 */
//	public int getSurplusForAgent(int modelID, int agentID) {
//		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
//		int surplus = 0;
//		
//		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
//			PQTransaction t = trans.getValue();
//			
//			if (ids.contains(t.marketID)) {
//				Agent buyer = agents.get(t.buyerID);
//				Agent seller = agents.get(t.sellerID);
//				
//				if (buyer.getID() == agentID) {
//					if (buyer.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						//surplus += (buyer.getPrivateValueAt(t.quantity).sum(rt)).diff(t.price).getPrice();
//						surplus += getPrivateValueByBid(t.buyBidID).sum(rt).diff(t.price).getPrice();
//					} else {
//						surplus = buyer.getRealizedProfit(); // already summed
//					}
//				}
//				if (seller.getID() == agentID) {
//					if (seller.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						//surplus += t.price.diff(seller.getPrivateValueAt(-t.quantity).sum(rt)).getPrice();
//						surplus += t.price.diff(getPrivateValueByBid(t.sellBidID).sum(rt)).getPrice();
//					} else {
//						surplus = seller.getRealizedProfit(); // already summed
//					}
//				}
//			}
//		}
//		return surplus;
//	}
	
	
//	/**
//	 * Iterates through all transactions and sums up surplus for all background agents
//	 * that have a private valuation.
//	 * 
//	 * Note that private values are a deviation from the fundamental (v).
//	 * 
//	 * CS = (PV + v) - p, PS = p - (PV + v)
//	 * 
//	 * @param modelID 		model id to check
//	 * @return hash map of background agent surplus, hashed by agent ID
//	 */
//	public HashMap<Integer,Integer> getBackgroundSurplus(int modelID) {
//		// basically the same as discounted surplus, but with discount 0.0
//		// also Integers instead of Doubles
//
//		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
//		HashMap<Integer,Integer> allSurplus = new HashMap<Integer,Integer>();
//		
//		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
//			PQTransaction t = trans.getValue();
//			
//			if (ids.contains(t.marketID)) {
//				Agent buyer = getAgent(t.buyerID);
//				Agent seller = getAgent(t.sellerID);
//				
//				// Check that PV is defined & that it is a background agent
//				if (isNonPlayer(buyer.getID())) {
//					int surplus = 0;
//					if (allSurplus.containsKey(buyer.getID())) {
//						surplus = allSurplus.get(buyer.getID());
//					}
//					if (buyer.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						//int val = (buyer.getPrivateValueAt(t.quantity).sum(rt)).diff(t.price).getPrice();
//						int val = getPrivateValueByBid(t.buyBidID).sum(rt).diff(t.price).getPrice();
//						allSurplus.put(buyer.getID(), surplus + val);	
//					} else {
//						allSurplus.put(buyer.getID(), buyer.getRealizedProfit());	// already summed
//					}
//				}
//				if (isNonPlayer(seller.getID())) {
//					int surplus = 0;
//					if (allSurplus.containsKey(seller.getID())) {
//						surplus = allSurplus.get(seller.getID());
//					}
//					if (seller.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						//int val = t.price.diff(seller.getPrivateValueAt(-t.quantity).sum(rt)).getPrice();
//						int val = t.price.diff(getPrivateValueByBid(t.sellBidID).sum(rt)).getPrice();
//						allSurplus.put(seller.getID(), surplus + val);
//					} else {
//						allSurplus.put(seller.getID(), seller.getRealizedProfit());	// already summed
//					}
//				}
//			}
//		}
//		return allSurplus;
//	}
//	
//	/**
//	 * Iterates through all transactions and sums up discounted surplus for all
//	 * background agents. (Note that HFT agent transactions will naturally execute
//	 * with zero transaction time, so they do not need to be discounted).
//	 * 
//	 * Each transaction's surplus is discounted by exp{-rho * T}, where T is the 
//	 * execution speed of that transaction.
//	 * 
//	 * CS = (PV + v) - p, PS = p - (PV + v)
//	 * 
//	 * @param modelID 		model id to check
//	 * @param rho			discount factor
//	 * @return discounted surplus, hashed by (background) agent ID
//	 */
//	public HashMap<Integer,Double> getDiscountedSurplus(int modelID, double rho) {
//		ArrayList<Integer> ids = getModel(modelID).getMarketIDs();
//		HashMap<Integer,Double> discSurplus = new HashMap<Integer,Double>();
//		
//		for (Map.Entry<Integer,PQTransaction> trans : getTrans(modelID).entrySet()) {
//			PQTransaction t = trans.getValue();
//			
//			if (ids.contains(t.marketID)) {
//				Agent buyer = agents.get(t.buyerID);
//				Agent seller = agents.get(t.sellerID);
//				TimeStamp buyTime = timeToExecution.get(t.buyBidID);
//				TimeStamp sellTime = timeToExecution.get(t.sellBidID);
//				
//				// Check that PV is defined & that it is a background agent
//				if (isNonPlayer(buyer.getID())) {
//					double surplus = 0;
//					if (discSurplus.containsKey(buyer.getID())) {
//						surplus = discSurplus.get(buyer.getID());
//					}
//					if (buyer.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						double cs = getPrivateValueByBid(t.buyBidID).sum(rt).diff(t.price).getPrice();
////						System.out.println(modelID + ": " + t + " cs=" + cs + ", buyTime=" + buyTime);
//						discSurplus.put(buyer.getID(), surplus + Math.exp(-rho * buyTime.longValue()) * cs);
//					} else {
//						discSurplus.put(buyer.getID(), Math.exp(-rho * buyTime.longValue()) * buyer.getRealizedProfit());
//					}
//				}
//				if (isNonPlayer(seller.getID())) {
//					double surplus = 0;
//					if (discSurplus.containsKey(seller.getID())) {
//						surplus = discSurplus.get(seller.getID());
//					}
//					if (seller.getPrivateValue() != null) {
//						Price rt = getFundamentalAt(t.timestamp);
//						double ps = t.price.diff(getPrivateValueByBid(t.sellBidID).sum(rt)).getPrice();
////						System.out.println(modelID + ": " + t + " ps=" + ps + ", sellTime=" + sellTime);
//						discSurplus.put(seller.getID(), surplus + Math.exp(-rho * sellTime.longValue()) * ps);
//					} else {
//						discSurplus.put(seller.getID(), Math.exp(-rho * sellTime.longValue()) * seller.getRealizedProfit());
//					}
//				}
//			}
//		}
//		return discSurplus;
//	}

	
	
	

	
	
}
