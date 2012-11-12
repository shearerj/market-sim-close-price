package models;

import java.util.ArrayList;
import java.util.Iterator;

import systemmanager.*;

/**
 * Base class for specifying a market model (e.g. two-market model, 
 * centralized call market, etc.).
 * 
 * Multiple market models can be included in a single simulation trial.
 * Agents present in the simulation will reside in a primary market model
 * and submit identical bids to each additional model.
 * 
 * If the market model has only one market, then it is assumed to be a
 * "centralized" model. Note that only market properties can be set here,
 * as there is only one copy of each agent in the simulation.
 * 
 * @author ewah
 */
public abstract class MarketModel {

	private ObjectProperties modelProperties;
	
	// Specify configuration of market type & properties
	protected ArrayList<MarketObjectPair> modelConfig;
	
	// Store information on market IDs for each market specified in modelProperties
	protected ArrayList<Integer> marketIDs;
	
	public MarketModel(ObjectProperties p) {
		modelConfig = new ArrayList<MarketObjectPair>();
		marketIDs = new ArrayList<Integer>();
		modelProperties = p;
	}
	
	/**
	 * Add a market-property pair to the MarketModel.
	 * 
	 * @param mktType
	 * @param mktProperties
	 */
	public void addMarketPropertyPair(String mktType, ObjectProperties mktProperties) {
		MarketObjectPair mpp = new MarketObjectPair(mktType, mktProperties);
		modelConfig.add(mpp);
	}
	
	/**
	 * Add a market with default property settings to the MarketModel.
	 * 
	 * @param mktType
	 */
	public void addMarketPropertyPair(String mktType) {
		ObjectProperties mktProperties = Consts.getProperties(mktType);
		MarketObjectPair mpp = new MarketObjectPair(mktType, mktProperties);
		modelConfig.add(mpp);
	}
	
	/**
	 * @param setup
	 * @param data
	 */
	public void createMarkets(SystemSetup setup, SystemData data) {
		for(Iterator<MarketObjectPair> it = modelConfig.iterator(); it.hasNext(); ) {
			MarketObjectPair mop = it.next();
			int mID = setup.nextMarketID();
			setup.setupMarket(mID, mop.getMarketType(), (ObjectProperties) mop.getObject(), this.hashCode());
			marketIDs.add(mID);
		}
	}
	
	/**
	 * @return marketIDs
	 */
	public ArrayList<Integer> getMarketIDs() {
		return marketIDs;
	}
	
//	/**
//	 * Returns the number of each market type in this market model.
//	 * 
//	 * @return HashMap hashed by market type & number of this market in this model
//	 */
//	public HashMap<String,Integer> getNumMarketType() {
//		HashMap<String,Integer> num = new HashMap<String,Integer>();
//		if (modelConfig.size() == 0) return num;
//		
//		for (Iterator<MarketObjectPair> it = modelConfig.iterator(); it.hasNext(); ) {
//			MarketObjectPair mpp = it.next();
//			if (num.get(mpp.getMarketType()) == null) {
//				// no mapping yet
//				num.put(mpp.getMarketType(), 1);
//			} else {
//				int tmp = num.get(mpp.getMarketType());
//				num.put(mpp.getMarketType(), tmp++);
//			}
//		}
//		return num;
//	}
}