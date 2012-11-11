package models;

import java.util.HashMap;
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

	protected SystemManager manager;
	
	// Specify configuration of market type & properties
	protected ArrayList<MarketObjectPair> modelProperties;
	
	// Store information on market IDs for each market specified in modelProperties
	protected ArrayList<Integer> marketIDs;
	
	public MarketModel() {
		modelProperties = new ArrayList<MarketObjectPair>();
		marketIDs = new ArrayList<Integer>();
	}
	
	/**
	 * Add a market-property pair to the MarketModel.
	 * 
	 * @param mktType
	 * @param mktProperties
	 */
	public void addMarketPropertyPair(String mktType, EntityProperties mktProperties) {
		MarketObjectPair mpp = new MarketObjectPair(mktType, mktProperties);
		modelProperties.add(mpp);
	}
	
	/**
	 * Add a market with default property settings to the MarketModel.
	 * 
	 * @param mktType
	 */
	public void addMarketPropertyPair(String mktType) {
		EntityProperties mktProperties = Consts.getProperties(mktType);
		MarketObjectPair mpp = new MarketObjectPair(mktType, mktProperties);
		modelProperties.add(mpp);
	}
	
	/**
	 * Initializes the market stored in the MarketObjectPair and adds its ID to the
	 * marketIDs array.
	 * 
	 * @param mop
	 */
	public void createMarket(MarketObjectPair mop) {
		// parse the MarketObjectPair & create it
		
//		int id = manager.setupMarket(
//		marketIDs.add(id);
	}
	
	/**
	 * Returns the number of each market type in this market model.
	 * 
	 * @return
	 */
	public HashMap<String,Integer> getNumMarketType() {
		HashMap<String,Integer> num = new HashMap<String,Integer>();
		if (modelProperties.size() == 0) return num;
		
		for (Iterator<MarketObjectPair> it = modelProperties.iterator(); it.hasNext(); ) {
			MarketObjectPair mpp = it.next();
			if (num.get(mpp.getMarketType()) == null) {
				// no mapping yet
				num.put(mpp.getMarketType(), 1);
			} else {
				int tmp = num.get(mpp.getMarketType());
				num.put(mpp.getMarketType(), tmp++);
			}
		}
		return num;
	}
	
	/**
	 * @return marketIDs
	 */
	public ArrayList<Integer> getMarketIDs() {
		return marketIDs;
	}
}